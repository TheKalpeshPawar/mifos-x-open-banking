/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.model.banking.payment.PaymentDisposition
import org.mifosx.openbanking.core.model.banking.payment.PaymentStatus
import org.mifosx.openbanking.feature.paymentstatus.FakePaymentInitiationRepository
import org.mifosx.openbanking.feature.paymentstatus.PaymentStatusFixtures
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PaymentStatusViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakePaymentInitiationRepository = FakePaymentInitiationRepository(),
        paymentId: String = PaymentStatusFixtures.PAYMENT_ID,
    ) = PaymentStatusViewModel(
        savedStateHandle = SavedStateHandle(mapOf(PaymentStatusViewModel.PAYMENT_ID_ARG to paymentId)),
        repository = repository,
    )

    private fun content(vm: PaymentStatusViewModel): PaymentStatusUiState.Content =
        assertIs<PaymentStatusUiState.Content>(vm.stateFlow.value.uiState)

    @Test
    fun readsTheStatusForTheRoutesPaymentId() = runTest {
        val repository = FakePaymentInitiationRepository()

        viewModel(repository)

        assertEquals(listOf(PaymentStatusFixtures.PAYMENT_ID), repository.statusReads)
    }

    @Test
    fun rendersTheReceiptTheBankEchoed() = runTest {
        val vm = viewModel()

        val state = content(vm)
        assertEquals("£850.00", state.amountLabel)
        assertEquals("Jameson Lettings", state.creditorName)
        assertEquals("RENT-FLAT12", state.reference)
        assertEquals(PaymentStatusFixtures.PAYMENT_ID, state.paymentId)
    }

    /** The wire carries fourteen unpunctuated digits; people read a sort code in pairs. */
    @Test
    fun formatsThePayingAccountTheWayItIsWrittenDown() = runTest {
        val vm = viewModel()

        assertEquals("40-05-15 12345678", content(vm).debtorLabel)
    }

    /**
     * Accepted-but-not-settled is the normal answer for a fresh payment. Rendering it as a fault
     * would send people chasing a problem that does not exist.
     */
    @Test
    fun treatsAnAcceptedPaymentAsInProgressRatherThanDone() = runTest {
        val vm = viewModel()

        val state = content(vm)
        assertEquals(PaymentDisposition.InProgress, state.disposition)
        assertTrue(state.inProgress)
    }

    @Test
    fun reportsASettledPaymentAsTerminalSuccess() = runTest {
        val repository = FakePaymentInitiationRepository()
        repository.statusReturns(
            NetworkResult.Success(
                PaymentStatusFixtures.receipt(status = PaymentStatus.AcceptedSettlementCompleted),
            ),
        )

        val state = content(viewModel(repository))
        assertEquals(PaymentDisposition.TerminalSuccess, state.disposition)
        assertFalse(state.inProgress)
    }

    @Test
    fun reportsARejectedPaymentAsTerminalFailure() = runTest {
        val repository = FakePaymentInitiationRepository()
        repository.statusReturns(
            NetworkResult.Success(PaymentStatusFixtures.receipt(status = PaymentStatus.Rejected)),
        )

        assertEquals(PaymentDisposition.TerminalFailure, content(viewModel(repository)).disposition)
    }

    @Test
    fun refreshingReReadsTheStatus() = runTest {
        val repository = FakePaymentInitiationRepository()
        val vm = viewModel(repository)

        vm.trySendAction(PaymentStatusAction.RefreshStatus)

        assertEquals(2, repository.statusReads.size)
    }

    /** Replacing a known answer with a skeleton reads as losing it. */
    @Test
    fun refreshingKeepsTheCurrentStatusOnScreen() = runTest {
        val repository = FakePaymentInitiationRepository()
        val vm = viewModel(repository)

        vm.trySendAction(PaymentStatusAction.RefreshStatus)

        assertIs<PaymentStatusUiState.Content>(vm.stateFlow.value.uiState)
    }

    /** A payment id that resolves to nothing is a failure to explain, not an empty set. */
    @Test
    fun anUnknownPaymentIsAnErrorRatherThanAnEmptyState() = runTest {
        val repository = FakePaymentInitiationRepository()
        repository.statusReturns(NetworkResult.Error(NetworkError.Client.NotFound(null)))

        val state = assertIs<PaymentStatusUiState.Error>(viewModel(repository).stateFlow.value.uiState)
        assertEquals(PaymentStatusErrorKind.PaymentNotFound, state.kind)
    }

    @Test
    fun anExpiredTokenIsReportedAsSuch() = runTest {
        val repository = FakePaymentInitiationRepository()
        repository.statusReturns(NetworkResult.Error(NetworkError.Client.Unauthorized(null)))

        val state = assertIs<PaymentStatusUiState.Error>(viewModel(repository).stateFlow.value.uiState)
        assertEquals(PaymentStatusErrorKind.TokenExpired, state.kind)
    }

    @Test
    fun aTransportFailureIsReportedAsANetworkError() = runTest {
        val repository = FakePaymentInitiationRepository()
        repository.statusReturns(
            NetworkResult.Error(NetworkError.Network(cause = RuntimeException("offline"))),
        )

        val state = assertIs<PaymentStatusUiState.Error>(viewModel(repository).stateFlow.value.uiState)
        assertEquals(PaymentStatusErrorKind.NetworkError, state.kind)
    }

    /** Retry from the error state is the same read, so a recovered payment renders normally. */
    @Test
    fun retryingAfterAFailureRecoversTheContent() = runTest {
        val repository = FakePaymentInitiationRepository()
        repository.statusReturns(
            NetworkResult.Error(NetworkError.Network(cause = RuntimeException("offline"))),
        )
        val vm = viewModel(repository)
        assertIs<PaymentStatusUiState.Error>(vm.stateFlow.value.uiState)

        repository.statusReturns(NetworkResult.Success(PaymentStatusFixtures.receipt()))
        vm.trySendAction(PaymentStatusAction.RefreshStatus)

        assertEquals("£850.00", content(vm).amountLabel)
    }
}
