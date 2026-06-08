/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.TransactionChallenge
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import template.core.base.store.screen.ScreenDataStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class ConfirmFakePaymentsRepository(
    var sepaResult: Result<TransactionRequest> = Result.success(TransactionRequest(status = "COMPLETED")),
    var counterpartyResult: Result<TransactionRequest> = Result.success(TransactionRequest(status = "COMPLETED")),
    var sandboxTanResult: Result<TransactionRequest> = Result.success(TransactionRequest(status = "COMPLETED")),
) : PaymentsRepository {
    var sepaCalls = 0
    var counterpartyCalls = 0
    var sandboxTanCalls = 0
    var lastSandboxTanTo: Pair<String, String>? = null

    override fun beneficiariesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Counterparty>> = TODO()
    override suspend fun listBeneficiaries(bankId: String, accountId: String): Result<List<Counterparty>> =
        Result.success(emptyList())
    override suspend fun listTransactionRequests(
        bankId: String,
        accountId: String,
    ): Result<List<TransactionRequestSummary>> = Result.success(emptyList())
    override suspend fun sendSepaPayment(
        bankId: String,
        accountId: String,
        iban: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> {
        sepaCalls++
        return sepaResult
    }
    override suspend fun sendToCounterparty(
        bankId: String,
        accountId: String,
        counterpartyId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> {
        counterpartyCalls++
        return counterpartyResult
    }
    override suspend fun fundsAvailable(
        bankId: String,
        accountId: String,
        amount: String,
        currency: String,
    ): Result<Boolean> = Result.success(true)
    override suspend fun sendToSandboxTan(
        bankId: String,
        accountId: String,
        toBankId: String,
        toAccountId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> {
        sandboxTanCalls++
        lastSandboxTanTo = toBankId to toAccountId
        return sandboxTanResult
    }
    override suspend fun answerChallenge(
        bankId: String,
        accountId: String,
        type: String,
        requestId: String,
        challengeId: String,
        answer: String,
    ): Result<TransactionRequest> = Result.success(TransactionRequest(status = "COMPLETED"))
}

private fun confirmDraft(type: PaymentType, iban: String = "DE89370400440532099999") = PaymentDraft(
    fromBankId = "ac.bank.uk",
    fromAccountId = "ac.checking.001",
    fromLabel = "Main",
    amount = "10.00",
    currency = "EUR",
    counterpartyId = "cp-1",
    beneficiaryName = "Payee",
    beneficiaryBank = "ac.bank.uk",
    iban = iban,
    reference = "test",
    paymentType = type,
)

class SendMoneyConfirmViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun sepaDraft_routesToSepaEndpoint() = runTest(dispatcher) {
        val payments = ConfirmFakePaymentsRepository()
        val model = SendMoneyConfirmViewModel(payments)
        var success = false
        model.submit(confirmDraft(PaymentType.SEPA), onCompleted = { success = true }, onChallengeRequired = {})
        advanceUntilIdle()
        assertTrue(success)
        assertEquals(1, payments.sepaCalls)
        assertEquals(0, payments.counterpartyCalls)
    }

    @Test
    fun domesticDraft_routesToCounterparty() = runTest(dispatcher) {
        val payments = ConfirmFakePaymentsRepository()
        val model = SendMoneyConfirmViewModel(payments)
        var success = false
        model.submit(confirmDraft(PaymentType.DOMESTIC), onCompleted = { success = true }, onChallengeRequired = {})
        advanceUntilIdle()
        assertTrue(success)
        assertEquals(0, payments.sepaCalls)
        assertEquals(1, payments.counterpartyCalls)
    }

    @Test
    fun sepaWithoutIban_fallsBackToCounterparty() = runTest(dispatcher) {
        val payments = ConfirmFakePaymentsRepository()
        val model = SendMoneyConfirmViewModel(payments)
        model.submit(confirmDraft(PaymentType.SEPA, iban = ""), onCompleted = {}, onChallengeRequired = {})
        advanceUntilIdle()
        assertEquals(0, payments.sepaCalls)
        assertEquals(1, payments.counterpartyCalls)
    }

    @Test
    fun sepaSandboxResolutionFailure_fallsBackToCounterparty() = runTest(dispatcher) {
        val payments = ConfirmFakePaymentsRepository(
            sepaResult = Result.failure(IllegalStateException("OBP-30012: Counterparty not found.")),
        )
        val model = SendMoneyConfirmViewModel(payments)
        var success = false
        model.submit(confirmDraft(PaymentType.SEPA), onCompleted = { success = true }, onChallengeRequired = {})
        advanceUntilIdle()
        assertTrue(success)
        assertEquals(1, payments.sepaCalls)
        assertEquals(1, payments.counterpartyCalls)
    }

    @Test
    fun sepaOtherFailure_doesNotFallBack() = runTest(dispatcher) {
        val payments = ConfirmFakePaymentsRepository(
            sepaResult = Result.failure(IllegalStateException("OBP-40003: currency mismatch")),
        )
        val model = SendMoneyConfirmViewModel(payments)
        model.submit(confirmDraft(PaymentType.SEPA), onCompleted = {}, onChallengeRequired = {})
        advanceUntilIdle()
        assertEquals(1, payments.sepaCalls)
        assertEquals(0, payments.counterpartyCalls)
        assertTrue(model.state.value is ConfirmUiState.Failed)
    }

    @Test
    fun initiatedWithChallenge_routesToChallenge() = runTest(dispatcher) {
        val payments = ConfirmFakePaymentsRepository(
            counterpartyResult = Result.success(
                TransactionRequest(
                    id = "req-1",
                    type = "COUNTERPARTY",
                    status = "INITIATED",
                    challenge = TransactionChallenge(id = "ch-1"),
                ),
            ),
        )
        val model = SendMoneyConfirmViewModel(payments)
        var completed = false
        var challenge: ScaChallengeArgs? = null
        model.submit(
            confirmDraft(PaymentType.DOMESTIC),
            onCompleted = { completed = true },
            onChallengeRequired = { challenge = it },
        )
        advanceUntilIdle()
        assertFalse(completed)
        assertEquals("req-1", challenge?.requestId)
        assertEquals("ch-1", challenge?.challengeId)
        assertEquals("COUNTERPARTY", challenge?.type)
    }

    @Test
    fun sandboxTanDraft_routesToSandboxTanRail() = runTest(dispatcher) {
        val payments = ConfirmFakePaymentsRepository()
        val model = SendMoneyConfirmViewModel(payments)
        var completed = false
        model.submit(
            confirmDraft(PaymentType.DOMESTIC).copy(
                useSandboxTan = true,
                toBankId = "ac.bank.uk",
                toAccountId = "ac.savings.001",
            ),
            onCompleted = { completed = true },
            onChallengeRequired = {},
        )
        advanceUntilIdle()
        assertTrue(completed)
        assertEquals(1, payments.sandboxTanCalls)
        assertEquals(0, payments.sepaCalls)
        assertEquals(0, payments.counterpartyCalls)
        assertEquals("ac.bank.uk" to "ac.savings.001", payments.lastSandboxTanTo)
    }
}
