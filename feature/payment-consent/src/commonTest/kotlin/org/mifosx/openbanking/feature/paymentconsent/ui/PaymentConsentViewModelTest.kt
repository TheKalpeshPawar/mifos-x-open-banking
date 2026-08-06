/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentconsent.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.callback.PaymentAuthValidation
import org.mifosx.openbanking.feature.paymentconsent.FakePaymentAuthRepository
import org.mifosx.openbanking.feature.paymentconsent.FakePaymentInitiationRepository
import org.mifosx.openbanking.feature.paymentconsent.PaymentConsentFixtures
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PaymentConsentViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakePaymentAuthRepository = FakePaymentAuthRepository(),
        payments: FakePaymentInitiationRepository = FakePaymentInitiationRepository(),
    ) = PaymentConsentViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(PaymentConsentViewModel.REDIRECT_URL_ARG to PaymentConsentFixtures.REDIRECT_URL),
        ),
        repository = repository,
        paymentInitiationRepository = payments,
    )

    // region — the happy path

    @Test
    fun anAuthenticCallbackExchangesTheCodeAndConfirmsTheConsent() = runTest {
        val repository = FakePaymentAuthRepository()

        viewModel(repository)

        assertEquals(listOf(PaymentConsentFixtures.CODE), repository.exchangedCodes)
        assertEquals(listOf(PaymentConsentFixtures.CONSENT_ID), repository.statusChecks)
    }

    /**
     * The whole point of the move: the leg that returns from the bank is the one that submits,
     * because the screen that built the draft no longer exists by the time the redirect lands.
     */
    @Test
    fun anAuthorisedConsentConfirmsFundsAndSubmitsTheStagedDraft() = runTest {
        val payments = FakePaymentInitiationRepository()

        viewModel(payments = payments)

        assertEquals(listOf(PaymentConsentFixtures.CONSENT_ID), payments.fundsChecks)
        assertEquals(listOf(PaymentConsentFixtures.draft()), payments.submittedDrafts)
    }

    @Test
    fun aSubmittedPaymentLeavesAsAnEventCarryingItsId() = runTest {
        val vm = viewModel()

        val event = vm.eventFlow.first()

        assertEquals(
            PaymentConsentEvent.PaymentSubmitted(PaymentConsentFixtures.PAYMENT_ID),
            event,
        )
    }

    /**
     * A completed payment must not leave its consent id, PSU token or draft behind: the next payment
     * would otherwise find a credential its own authorisation never issued.
     */
    @Test
    fun aSubmittedPaymentClearsTheAuthorisation() = runTest {
        val repository = FakePaymentAuthRepository()

        viewModel(repository)

        assertEquals(1, repository.discardCount)
    }

    /** HSBC reports it as `AUTH`; some responses spell it out. Both mean authorised. */
    @Test
    fun acceptsEitherSpellingOfAnAuthorisedConsent() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.statusReturns(NetworkResult.Success("Authorised"))
        val payments = FakePaymentInitiationRepository()

        viewModel(repository, payments)

        assertEquals(1, payments.submittedDrafts.size)
    }

    // endregion

    // region — the funds gate

    /**
     * The protocol calls funds confirmation optional, but once made its answer is binding: submitting
     * anyway would knowingly send a payment the bank has just said cannot be covered.
     */
    @Test
    fun aNegativeFundsCheckStopsBeforeSubmitting() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.fundsReturn(NetworkResult.Success(false))

        val vm = viewModel(payments = payments)

        assertTrue(payments.submittedDrafts.isEmpty())
        val state = assertIs<PaymentConsentUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(PaymentConsentErrorKind.InsufficientFunds, state.kind)
    }

    /**
     * Without the staged draft there is nothing to send. Rebuilding one here would submit an
     * `Initiation` the consent was never granted against, so this fails closed instead.
     */
    @Test
    fun aMissingStagedDraftIsTerminalRatherThanRebuilt() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.stagedDraftReturns(null)

        val vm = viewModel(payments = payments)

        assertTrue(payments.fundsChecks.isEmpty())
        assertTrue(payments.submittedDrafts.isEmpty())
        val state = assertIs<PaymentConsentUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(PaymentConsentErrorKind.NoStagedPayment, state.kind)
    }

    @Test
    fun aRefusedSubmissionSurfacesAsSubmissionFailed() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.submissionReturns(
            NetworkResult.Error(NetworkError.Client.BadRequest("U008")),
        )

        val vm = viewModel(payments = payments)

        val state = assertIs<PaymentConsentUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(PaymentConsentErrorKind.SubmissionFailed, state.kind)
    }

    // endregion

    // region — the poll gate

    /**
     * Submitting against a consent that has not reached AUTH returns 400 U009, so a consent still
     * awaiting authorisation must not be handed back as authorised.
     */
    @Test
    fun aConsentStillAwaitingAuthorisationIsNotHandedBack() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.statusReturns(NetworkResult.Success("AWAU"))

        val state = assertIs<PaymentConsentUiState.Checking>(viewModel(repository).stateFlow.value.uiState)
        assertTrue(state.canCheckAgain)
    }

    /** A slow bank is normal, so the way out is another look rather than an error. */
    @Test
    fun checkingAgainRepollsTheConsentStatus() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.statusReturns(NetworkResult.Success("AWAU"))
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(repository, payments)

        repository.statusReturns(NetworkResult.Success("AUTH"))
        vm.trySendAction(PaymentConsentAction.CheckAgain)

        assertEquals(2, repository.statusChecks.size)
        assertEquals(1, payments.submittedDrafts.size)
    }

    // endregion

    // region — replay and refusal

    /**
     * A callback whose `state` does not match the authorisation this app launched is a replay
     * signal, not a transient fault — it fails closed and is never retried through.
     */
    @Test
    fun aMismatchedStateIsRefusedWithoutExchangingAnything() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.validationReturns(PaymentAuthValidation.SecurityError)

        val vm = viewModel(repository)

        val state = assertIs<PaymentConsentUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(PaymentConsentErrorKind.StateMismatch, state.kind)
        assertTrue(repository.exchangedCodes.isEmpty())
        assertTrue(repository.statusChecks.isEmpty())
    }

    @Test
    fun aDeclinedConsentIsReportedAsRejected() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.validationReturns(PaymentAuthValidation.AccessDenied)

        val state = assertIs<PaymentConsentUiState.Error>(viewModel(repository).stateFlow.value.uiState)
        assertEquals(PaymentConsentErrorKind.ConsentRejected, state.kind)
    }

    @Test
    fun aMissingCodeIsReportedAsExpired() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.validationReturns(PaymentAuthValidation.MissingCode)

        val state = assertIs<PaymentConsentUiState.Error>(viewModel(repository).stateFlow.value.uiState)
        assertEquals(PaymentConsentErrorKind.CodeExpired, state.kind)
    }

    @Test
    fun aFailedExchangeSurfacesAsAnError() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.exchangeReturns(NetworkResult.Error(NetworkError.Client.Unauthorized(null)))

        val state = assertIs<PaymentConsentUiState.Error>(viewModel(repository).stateFlow.value.uiState)
        assertEquals(PaymentConsentErrorKind.CodeExpired, state.kind)
        assertTrue(repository.statusChecks.isEmpty())
    }

    @Test
    fun aFailedStatusReadSurfacesAsAnError() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.statusReturns(
            NetworkResult.Error(NetworkError.Network(cause = RuntimeException("offline"))),
        )

        val state = assertIs<PaymentConsentUiState.Error>(viewModel(repository).stateFlow.value.uiState)
        assertEquals(PaymentConsentErrorKind.NetworkError, state.kind)
    }

    // endregion

    // region — the two exits

    @Test
    fun restartingAsksTheHostToAuthoriseAgain() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.validationReturns(PaymentAuthValidation.SecurityError)
        val vm = viewModel(repository)

        vm.trySendAction(PaymentConsentAction.RetryAuthorisation)

        assertEquals(PaymentConsentEvent.RestartAuthorisation, vm.eventFlow.first())
    }

    /** A stuck authorisation gets a clean exit rather than leaving someone to back out. */
    @Test
    fun abandoningReportsThePaymentAsAbandoned() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.validationReturns(PaymentAuthValidation.SecurityError)
        val vm = viewModel(repository)

        vm.trySendAction(PaymentConsentAction.AbandonPayment)

        assertEquals(PaymentConsentEvent.Abandoned, vm.eventFlow.first())
    }

    // endregion
}
