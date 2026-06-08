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
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import template.core.base.store.screen.ScreenDataStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class ChallengeFakePaymentsRepository(
    var answerResult: Result<TransactionRequest> = Result.success(TransactionRequest(status = "COMPLETED")),
) : PaymentsRepository {
    var lastType: String? = null
    var lastRequestId: String? = null
    var lastChallengeId: String? = null
    var lastAnswer: String? = null

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
    ): Result<TransactionRequest> = Result.success(TransactionRequest())
    override suspend fun sendToCounterparty(
        bankId: String,
        accountId: String,
        counterpartyId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> = Result.success(TransactionRequest())
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
    ): Result<TransactionRequest> = Result.success(TransactionRequest())
    override suspend fun answerChallenge(
        bankId: String,
        accountId: String,
        type: String,
        requestId: String,
        challengeId: String,
        answer: String,
    ): Result<TransactionRequest> {
        lastType = type
        lastRequestId = requestId
        lastChallengeId = challengeId
        lastAnswer = answer
        return answerResult
    }
}

class ScaChallengeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: PaymentsRepository) = ScaChallengeViewModel(
        paymentsRepository = repo,
        bankId = "ac.bank.uk",
        accountId = "ac.checking.001",
        type = "COUNTERPARTY",
        requestId = "req-1",
        challengeId = "ch-1",
    )

    @Test
    fun correctAnswer_completesAndForwardsArgs() = runTest(dispatcher) {
        val repo = ChallengeFakePaymentsRepository()
        val model = vm(repo)
        model.onAnswerChanged("123")
        var completed = false
        model.onSubmit { completed = true }
        advanceUntilIdle()
        assertTrue(completed)
        assertEquals("123", repo.lastAnswer)
        assertEquals("ch-1", repo.lastChallengeId)
        assertEquals("req-1", repo.lastRequestId)
        assertEquals("COUNTERPARTY", repo.lastType)
        assertNull(model.state.value.errorMessage)
    }

    @Test
    fun exhaustedAttempts_showsErrorAndDoesNotComplete() = runTest(dispatcher) {
        val repo = ChallengeFakePaymentsRepository(
            answerResult = Result.failure(IllegalStateException("OBP-40014: used up your allowed attempts")),
        )
        val model = vm(repo)
        model.onAnswerChanged("000")
        var completed = false
        model.onSubmit { completed = true }
        advanceUntilIdle()
        assertFalse(completed)
        assertEquals(
            "Too many incorrect attempts. Please start the payment again.",
            model.state.value.errorMessage,
        )
        assertFalse(model.state.value.isSubmitting)
    }

    @Test
    fun nonCompletedStatus_showsError() = runTest(dispatcher) {
        val repo = ChallengeFakePaymentsRepository(
            answerResult = Result.success(TransactionRequest(status = "INITIATED")),
        )
        val model = vm(repo)
        model.onAnswerChanged("123")
        var completed = false
        model.onSubmit { completed = true }
        advanceUntilIdle()
        assertFalse(completed)
        assertEquals("Couldn't confirm the payment. Please try again.", model.state.value.errorMessage)
    }

    @Test
    fun blankAnswer_isNoOp() = runTest(dispatcher) {
        val repo = ChallengeFakePaymentsRepository()
        val model = vm(repo)
        model.onSubmit { }
        advanceUntilIdle()
        assertNull(repo.lastAnswer)
    }
}
