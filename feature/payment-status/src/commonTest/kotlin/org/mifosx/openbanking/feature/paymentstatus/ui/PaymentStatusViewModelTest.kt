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
import kotlinx.datetime.TimeZone
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
import kotlin.time.Clock
import kotlin.time.Instant

class PaymentStatusViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** A clock that can be wound forward, so "last checked" is assertable rather than wall-clock. */
    private class FixedClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private val clock = FixedClock(Instant.parse("2026-08-03T14:25:00Z"))

    private fun viewModel(
        repository: FakePaymentInitiationRepository = FakePaymentInitiationRepository(),
        paymentId: String = PaymentStatusFixtures.PAYMENT_ID,
    ) = PaymentStatusViewModel(
        savedStateHandle = SavedStateHandle(mapOf(PaymentStatusViewModel.PAYMENT_ID_ARG to paymentId)),
        repository = repository,
        clock = clock,
        // Fixed, so these assertions do not change meaning on a machine in another zone.
        timeZone = TimeZone.UTC,
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

    /**
     * The defect this rewrite exists for. "Submitted" was fed from `StatusUpdateDateTime`, which is
     * when the status last moved — a different fact from when the payment was made, and one HSBC
     * happens to return equal, which is exactly why the mislabel went unnoticed.
     */
    @Test
    fun submittedComesFromCreationTimeNotTheStatusUpdateTime() = runTest {
        val repository = FakePaymentInitiationRepository(
            receipt = PaymentStatusFixtures.receiptWithDistinctTimestamps(),
        )

        val state = content(viewModel(repository))

        assertEquals("3 Aug 2026, 14:22", state.submittedAt)
        assertEquals("3 Aug 2026, 16:40", state.statusChangedAt)
        assertEquals("4 Aug 2026, 09:00", state.settledAt)
    }

    @Test
    fun carriesTheChargeTheBankActuallyApplied() = runTest {
        val charge = content(viewModel()).charges.single()

        assertEquals("UK.OBIE.CHAPSOut", charge.typeLabel)
        assertEquals("£0.05", charge.amountLabel)
    }

    /** No charge is not the same claim as a zero charge, so nothing is invented to fill the gap. */
    @Test
    fun aPaymentWithNoChargesCarriesNone() = runTest {
        val repository = FakePaymentInitiationRepository(
            receipt = PaymentStatusFixtures.receipt(charges = emptyList()),
        )

        assertTrue(content(viewModel(repository)).charges.isEmpty())
    }

    @Test
    fun aMissingSettlementTimeLeavesTheRowEmptyRatherThanFormattingNothing() = runTest {
        val repository = FakePaymentInitiationRepository(
            receipt = PaymentStatusFixtures.receipt().copy(settlementDateTime = ""),
        )

        assertEquals("", content(viewModel(repository)).settledAt)
    }

    /**
     * The question that started this: a refresh returning the same status must still be visibly a
     * refresh. Without a moving "last checked" the button is indistinguishable from a dead one.
     */
    @Test
    fun refreshingUpdatesLastCheckedEvenWhenTheStatusHasNotMoved() = runTest {
        val repository = FakePaymentInitiationRepository()
        val vm = viewModel(repository)
        assertEquals("14:25", content(vm).lastCheckedAt)

        clock.instant = Instant.parse("2026-08-03T14:31:00Z")
        vm.trySendAction(PaymentStatusAction.RefreshStatus)

        val state = content(vm)
        assertEquals("14:31", state.lastCheckedAt)
        assertEquals(PaymentStatus.AcceptedSettlementInProcess, state.status)
    }

    /** And when it has moved, both the status and the stamp advance. */
    @Test
    fun aSettledReadUpdatesBothTheStatusAndTheStamp() = runTest {
        val repository = FakePaymentInitiationRepository()
        val vm = viewModel(repository)

        repository.receiptReturns(
            PaymentStatusFixtures.receipt(status = PaymentStatus.AcceptedCreditSettlementCompleted),
        )
        clock.instant = Instant.parse("2026-08-03T15:00:00Z")
        vm.trySendAction(PaymentStatusAction.RefreshStatus)

        val state = content(vm)
        assertEquals(PaymentStatus.AcceptedCreditSettlementCompleted, state.status)
        assertEquals(PaymentDisposition.TerminalSuccess, state.disposition)
        assertEquals("15:00", state.lastCheckedAt)
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
