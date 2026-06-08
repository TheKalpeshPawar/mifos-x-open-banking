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

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.ChallengeAnswerBody
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.CounterpartyTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.CounterpartyTransferTo
import org.mifosx.openbanking.core.model.obp.SandboxTanTo
import org.mifosx.openbanking.core.model.obp.SandboxTanTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.SepaCounterpartyIban
import org.mifosx.openbanking.core.model.obp.SepaTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import org.mifosx.openbanking.core.network.api.PaymentsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read access to an account's OBP counterparties (payees / beneficiaries). */
interface PaymentsRepository {
    /** Durable, offline-first stream of an account's beneficiaries (cache-then-network). */
    fun beneficiariesStream(accountId: String, scope: CoroutineScope): ScreenDataStream<List<Counterparty>>

    /**
     * List an account's beneficiaries. The OBP counterparties endpoint is path-scoped by bank,
     * so [bankId] must be the account's own bank (accounts can live on different banks) — passing
     * a wrong bank yields OBP-30018 "Bank Account not found".
     */
    suspend fun listBeneficiaries(bankId: String, accountId: String): Result<List<Counterparty>>

    /**
     * Past transaction-requests for the account at [bankId] — the authoritative record of which
     * counterparty/IBAN was paid and when. Used to derive "recent recipients" server-side.
     */
    suspend fun listTransactionRequests(bankId: String, accountId: String): Result<List<TransactionRequestSummary>>

    /** Send a SEPA payment from [accountId] (at [bankId]) to [iban]. */
    suspend fun sendSepaPayment(
        bankId: String,
        accountId: String,
        iban: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest>

    /** Send a payment from [accountId] (at [bankId]) to a registered counterparty by id. */
    suspend fun sendToCounterparty(
        bankId: String,
        accountId: String,
        counterpartyId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest>

    /**
     * Sandbox-only: send to an OBP-hosted account ([toBankId], [toAccountId]) via the SANDBOX_TAN
     * rail, whose challenge the maker can self-answer (unlike SEPA/COUNTERPARTY maker/checker).
     */
    suspend fun sendToSandboxTan(
        bankId: String,
        accountId: String,
        toBankId: String,
        toAccountId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest>

    /** True when the account has the requested funds available. */
    suspend fun fundsAvailable(
        bankId: String,
        accountId: String,
        amount: String,
        currency: String,
    ): Result<Boolean>

    /**
     * Answers the SCA challenge for an INITIATED transaction-request, completing the payment. [type]
     * is the transaction-request type (SEPA / COUNTERPARTY), [requestId] the request id and
     * [challengeId] the challenge id — both returned by the create-request response.
     */
    suspend fun answerChallenge(
        bankId: String,
        accountId: String,
        type: String,
        requestId: String,
        challengeId: String,
        answer: String,
    ): Result<TransactionRequest>
}

class PaymentsRepositoryImpl(
    private val api: PaymentsApi,
    private val config: ObpConfig,
    private val counterpartiesStore: Store<String, List<Counterparty>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : PaymentsRepository {

    override fun beneficiariesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Counterparty>> =
        counterpartiesStore.asScreenStream(
            key = accountId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "counterparties:$accountId",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun listBeneficiaries(bankId: String, accountId: String): Result<List<Counterparty>> =
        api.listCounterparties(bankId.ifBlank { config.bankId }, accountId)
            .toResult().map { it.counterparties }

    override suspend fun listTransactionRequests(
        bankId: String,
        accountId: String,
    ): Result<List<TransactionRequestSummary>> =
        api.listTransactionRequests(bankId.ifBlank { config.bankId }, accountId)
            .toResult().map { it.requests }

    override suspend fun sendSepaPayment(
        bankId: String,
        accountId: String,
        iban: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> {
        val result = api.createSepaTransactionRequest(
            bankId = bankId,
            accountId = accountId,
            request = SepaTransactionRequestBody(
                to = SepaCounterpartyIban(iban = iban),
                value = AmountOfMoney(currency = currency, amount = amount),
                description = reference.ifBlank { "Payment" },
            ),
        ).toResult()
        return enrichChallenge(result, bankId, accountId)
    }

    override suspend fun sendToCounterparty(
        bankId: String,
        accountId: String,
        counterpartyId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> {
        val result = api.createCounterpartyTransactionRequest(
            bankId = bankId,
            accountId = accountId,
            request = CounterpartyTransactionRequestBody(
                to = CounterpartyTransferTo(counterpartyId = counterpartyId),
                value = AmountOfMoney(currency = currency, amount = amount),
                description = reference.ifBlank { "Payment" },
            ),
        ).toResult()
        return enrichChallenge(result, bankId, accountId)
    }

    /**
     * The create response does not inline the SCA challenge, so an INITIATED payment with no
     * challenge is re-fetched here to populate its `challenge.id` — the value needed to answer it.
     */
    private suspend fun enrichChallenge(
        result: Result<TransactionRequest>,
        bankId: String,
        accountId: String,
    ): Result<TransactionRequest> {
        val request = result.getOrNull()
        val needsLookup = request != null &&
            request.status.equals("INITIATED", ignoreCase = true) &&
            request.challenge == null &&
            request.id.isNotBlank()
        if (!needsLookup) return result
        val fetched = api.getTransactionRequest(bankId, accountId, request!!.id).toResult().getOrNull()
        return if (fetched?.challenge != null) {
            Result.success(request.copy(challenge = fetched.challenge))
        } else {
            result
        }
    }

    override suspend fun fundsAvailable(
        bankId: String,
        accountId: String,
        amount: String,
        currency: String,
    ): Result<Boolean> =
        api.checkFundsAvailable(bankId, accountId, amount, currency)
            .toResult()
            .map { it.answer.equals("yes", ignoreCase = true) }

    override suspend fun sendToSandboxTan(
        bankId: String,
        accountId: String,
        toBankId: String,
        toAccountId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> {
        val result = api.createSandboxTanTransactionRequest(
            bankId = bankId,
            accountId = accountId,
            request = SandboxTanTransactionRequestBody(
                to = SandboxTanTo(bankId = toBankId, accountId = toAccountId),
                value = AmountOfMoney(currency = currency, amount = amount),
                description = reference.ifBlank { "Payment" },
            ),
        ).toResult()
        return enrichChallenge(result, bankId, accountId)
    }

    /**
     * SANDBOX_TAN challenges are answered through the v2.1.0 endpoint (no maker/checker); all other
     * rails use the v4.0.0 endpoint.
     */
    override suspend fun answerChallenge(
        bankId: String,
        accountId: String,
        type: String,
        requestId: String,
        challengeId: String,
        answer: String,
    ): Result<TransactionRequest> {
        val body = ChallengeAnswerBody(id = challengeId, answer = answer)
        return if (type.equals("SANDBOX_TAN", ignoreCase = true)) {
            api.answerSandboxTanChallenge(bankId, accountId, requestId, body).toResult()
        } else {
            api.answerTransactionRequestChallenge(bankId, accountId, type, requestId, body).toResult()
        }
    }
}
