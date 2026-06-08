/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.payments

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.testutil.FakeObpCacheDao
import org.mifosx.openbanking.core.data.testutil.NoopFetchedAt
import org.mifosx.openbanking.core.data.testutil.testJson
import org.mifosx.openbanking.core.data.testutil.testNetworkMonitor
import org.mifosx.openbanking.core.model.obp.ChallengeAnswerBody
import org.mifosx.openbanking.core.model.obp.CounterpartiesResponse
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.CounterpartyTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.FundsAvailableResponse
import org.mifosx.openbanking.core.model.obp.SandboxTanTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.SepaTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.TransactionChallenge
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestsResponse
import org.mifosx.openbanking.core.network.api.PaymentsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.payments.provideCounterpartiesStore
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakePaymentsApi(
    var listResult: NetworkResult<CounterpartiesResponse, NetworkError> =
        NetworkResult.Success(CounterpartiesResponse()),
    var requestsResult: NetworkResult<TransactionRequestsResponse, NetworkError> =
        NetworkResult.Success(TransactionRequestsResponse()),
    var sepaResult: NetworkResult<TransactionRequest, NetworkError> =
        NetworkResult.Success(TransactionRequest()),
    var fundsResult: NetworkResult<FundsAvailableResponse, NetworkError> =
        NetworkResult.Success(FundsAvailableResponse(answer = "yes")),
    var getRequestResult: NetworkResult<TransactionRequest, NetworkError> =
        NetworkResult.Success(TransactionRequest()),
    var answerResult: NetworkResult<TransactionRequest, NetworkError> =
        NetworkResult.Success(TransactionRequest()),
    var sandboxTanResult: NetworkResult<TransactionRequest, NetworkError> =
        NetworkResult.Success(TransactionRequest()),
) : PaymentsApi {
    override suspend fun listCounterparties(bankId: String, accountId: String) = listResult
    override suspend fun listTransactionRequests(bankId: String, accountId: String) = requestsResult
    override suspend fun getTransactionRequest(bankId: String, accountId: String, requestId: String) = getRequestResult
    override suspend fun createSepaTransactionRequest(
        bankId: String,
        accountId: String,
        request: SepaTransactionRequestBody,
    ) = sepaResult
    override suspend fun createCounterpartyTransactionRequest(
        bankId: String,
        accountId: String,
        request: CounterpartyTransactionRequestBody,
    ) = sepaResult
    override suspend fun checkFundsAvailable(
        bankId: String,
        accountId: String,
        amount: String,
        currency: String,
    ) = fundsResult
    var lastAnswerEndpoint: String? = null
    override suspend fun answerTransactionRequestChallenge(
        bankId: String,
        accountId: String,
        type: String,
        requestId: String,
        request: ChallengeAnswerBody,
    ): NetworkResult<TransactionRequest, NetworkError> {
        lastAnswerEndpoint = "v4"
        return answerResult
    }
    override suspend fun createSandboxTanTransactionRequest(
        bankId: String,
        accountId: String,
        request: SandboxTanTransactionRequestBody,
    ) = sandboxTanResult
    override suspend fun answerSandboxTanChallenge(
        bankId: String,
        accountId: String,
        requestId: String,
        request: ChallengeAnswerBody,
    ): NetworkResult<TransactionRequest, NetworkError> {
        lastAnswerEndpoint = "v2.1-sandboxtan"
        return answerResult
    }
}

private fun paymentsRepo(api: PaymentsApi): PaymentsRepositoryImpl {
    val config = ObpConfig()
    return PaymentsRepositoryImpl(
        api,
        config,
        provideCounterpartiesStore(api, config, FakeObpCacheDao(), testJson()),
        testNetworkMonitor(),
        NoopFetchedAt,
    )
}

class PaymentsRepositoryTest {

    @Test
    fun listBeneficiaries_unwrapsList() = runTest {
        val repo = paymentsRepo(
            FakePaymentsApi(
                NetworkResult.Success(
                    CounterpartiesResponse(counterparties = listOf(Counterparty(counterpartyId = "b1"))),
                ),
            ),
        )
        val result = repo.listBeneficiaries("bank-1", "acc-1")
        assertTrue(result.isSuccess)
        assertEquals("b1", result.getOrNull()?.firstOrNull()?.counterpartyId)
    }

    @Test
    fun listBeneficiaries_error_isFailure() = runTest {
        val repo = paymentsRepo(FakePaymentsApi(NetworkResult.Error(NetworkError.SERVER)))
        assertTrue(repo.listBeneficiaries("bank-1", "acc-1").isFailure)
    }

    @Test
    fun sendSepaPayment_success_returnsRequest() = runTest {
        val repo = paymentsRepo(
            FakePaymentsApi(
                sepaResult = NetworkResult.Success(
                    TransactionRequest(id = "tr-1", status = "COMPLETED"),
                ),
            ),
        )
        val result = repo.sendSepaPayment("ac.bank.uk", "acc-1", "GB29NWBK60161331926819", "10.00", "EUR", "rent")
        assertTrue(result.isSuccess)
        assertEquals("COMPLETED", result.getOrNull()?.status)
    }

    @Test
    fun sendToCounterparty_success_returnsRequest() = runTest {
        val repo = paymentsRepo(
            FakePaymentsApi(
                sepaResult = NetworkResult.Success(TransactionRequest(id = "tr-2", status = "COMPLETED")),
            ),
        )
        val result = repo.sendToCounterparty("ac.bank.uk", "acc-1", "cp-1", "10.00", "EUR", "rent")
        assertTrue(result.isSuccess)
        assertEquals("COMPLETED", result.getOrNull()?.status)
    }

    @Test
    fun sendToCounterparty_error_isFailure() = runTest {
        val repo = paymentsRepo(FakePaymentsApi(sepaResult = NetworkResult.Error(NetworkError.SERVER)))
        assertTrue(repo.sendToCounterparty("ac.bank.uk", "acc-1", "cp-1", "10.00", "EUR", "r").isFailure)
    }

    @Test
    fun send_initiatedWithoutChallenge_refetchesChallenge() = runTest {
        val repo = paymentsRepo(
            FakePaymentsApi(
                sepaResult = NetworkResult.Success(
                    TransactionRequest(id = "tr-3", type = "COUNTERPARTY", status = "INITIATED"),
                ),
                getRequestResult = NetworkResult.Success(
                    TransactionRequest(
                        id = "tr-3",
                        status = "INITIATED",
                        challenge = TransactionChallenge(id = "ch-9"),
                    ),
                ),
            ),
        )
        val result = repo.sendToCounterparty("ac.bank.uk", "acc-1", "cp-1", "5000", "EUR", "rent")
        assertEquals("ch-9", result.getOrNull()?.challenge?.id)
        assertTrue(result.getOrNull()?.requiresChallenge == true)
    }

    @Test
    fun sendToSandboxTan_success_returnsRequest() = runTest {
        val repo = paymentsRepo(
            FakePaymentsApi(
                sandboxTanResult = NetworkResult.Success(TransactionRequest(id = "tr-st", status = "COMPLETED")),
            ),
        )
        val result = repo.sendToSandboxTan("ac.bank.uk", "acc-1", "ac.bank.uk", "ac.savings.001", "1200", "EUR", "r")
        assertTrue(result.isSuccess)
        assertEquals("COMPLETED", result.getOrNull()?.status)
    }

    @Test
    fun answerChallenge_routesByType() = runTest {
        val api = FakePaymentsApi()
        val repo = paymentsRepo(api)
        repo.answerChallenge("ac.bank.uk", "acc-1", "SANDBOX_TAN", "tr-1", "ch-1", "123")
        assertEquals("v2.1-sandboxtan", api.lastAnswerEndpoint)
        repo.answerChallenge("ac.bank.uk", "acc-1", "COUNTERPARTY", "tr-2", "ch-2", "123")
        assertEquals("v4", api.lastAnswerEndpoint)
    }

    @Test
    fun fundsAvailable_mapsAnswerYesToTrue() = runTest {
        val yes = paymentsRepo(FakePaymentsApi(fundsResult = NetworkResult.Success(FundsAvailableResponse("yes"))))
        val no = paymentsRepo(FakePaymentsApi(fundsResult = NetworkResult.Success(FundsAvailableResponse("no"))))
        assertEquals(true, yes.fundsAvailable("ac.bank.uk", "acc-1", "10.00", "EUR").getOrNull())
        assertEquals(false, no.fundsAvailable("ac.bank.uk", "acc-1", "10.00", "EUR").getOrNull())
    }
}
