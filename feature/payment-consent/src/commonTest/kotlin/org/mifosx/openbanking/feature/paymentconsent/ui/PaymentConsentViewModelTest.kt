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
    ) = PaymentConsentViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(PaymentConsentViewModel.REDIRECT_URL_ARG to PaymentConsentFixtures.REDIRECT_URL),
        ),
        repository = repository,
    )

    // region — the happy path

    @Test
    fun anAuthenticCallbackExchangesTheCodeAndConfirmsTheConsent() = runTest {
        val repository = FakePaymentAuthRepository()

        val vm = viewModel(repository)

        assertEquals(listOf(PaymentConsentFixtures.CODE), repository.exchangedCodes)
        assertEquals(listOf(PaymentConsentFixtures.CONSENT_ID), repository.statusChecks)
        assertIs<PaymentConsentUiState.Authorised>(vm.stateFlow.value.uiState)
    }

    /** The payment is send-money's to finish, so the outcome leaves as an event, not a route. */
    @Test
    fun successIsHandedBackAsAnEvent() = runTest {
        val vm = viewModel()

        val event = vm.eventFlow.first()

        assertEquals(PaymentConsentEvent.Authorised(PaymentConsentFixtures.CONSENT_ID), event)
    }

    /** HSBC reports it as `AUTH`; some responses spell it out. Both mean authorised. */
    @Test
    fun acceptsEitherSpellingOfAnAuthorisedConsent() = runTest {
        val repository = FakePaymentAuthRepository()
        repository.statusReturns(NetworkResult.Success("Authorised"))

        assertIs<PaymentConsentUiState.Authorised>(viewModel(repository).stateFlow.value.uiState)
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
        val vm = viewModel(repository)

        repository.statusReturns(NetworkResult.Success("AUTH"))
        vm.trySendAction(PaymentConsentAction.CheckAgain)

        assertEquals(2, repository.statusChecks.size)
        assertIs<PaymentConsentUiState.Authorised>(vm.stateFlow.value.uiState)
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
