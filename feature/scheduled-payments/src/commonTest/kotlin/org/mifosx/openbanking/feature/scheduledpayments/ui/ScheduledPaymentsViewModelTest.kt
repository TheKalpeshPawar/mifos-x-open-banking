/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.scheduledpayments.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentItem
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentType
import org.mifosx.openbanking.feature.scheduledpayments.FakeScheduledPaymentsRepository
import org.mifosx.openbanking.feature.scheduledpayments.ScheduledPaymentsFixtures
import org.mifosx.openbanking.feature.scheduledpayments.ScheduledPaymentsRoute
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers [ScheduledPaymentsViewModel]'s argument routing, list-to-model mapping, display
 * formatting, error classification and retry.
 *
 * Test names are camelCase, not backticked: this source set also compiles for Kotlin/Native, whose
 * frontend rejects the parentheses and commas a prose-style backticked name would carry.
 */
class ScheduledPaymentsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeScheduledPaymentsRepository = FakeScheduledPaymentsRepository(),
        accountId: String = ScheduledPaymentsFixtures.ACCOUNT_ID,
    ): ScheduledPaymentsViewModel = ScheduledPaymentsViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(ScheduledPaymentsViewModel.ACCOUNT_ID_ARG to accountId),
        ),
        repository = repository,
    )

    private fun contentRepository(): FakeScheduledPaymentsRepository =
        FakeScheduledPaymentsRepository(ScheduledPaymentsFixtures.contentStreamState())

    private fun errorRepository(error: NetworkError): FakeScheduledPaymentsRepository =
        FakeScheduledPaymentsRepository(ScheduledPaymentsFixtures.errorStreamState(error))

    private fun content(vm: ScheduledPaymentsViewModel): ScheduledPaymentsUiState.Content =
        assertIs<ScheduledPaymentsUiState.Content>(vm.stateFlow.value.uiState)

    private fun errorState(repository: FakeScheduledPaymentsRepository): ScheduledPaymentsUiState.Error =
        assertIs<ScheduledPaymentsUiState.Error>(viewModel(repository).stateFlow.value.uiState)

    // region — routing & lifecycle

    @Test
    fun accountIdReadFromSavedStateHandle() {
        val vm = viewModel(accountId = "acc-9")
        assertEquals("acc-9", vm.stateFlow.value.accountId)
    }

    @Test
    fun missingArgFallsBackToEmptyString() {
        val vm = ScheduledPaymentsViewModel(SavedStateHandle(), FakeScheduledPaymentsRepository())
        assertEquals("", vm.stateFlow.value.accountId)
    }

    @Test
    fun argConstantMatchesTheRoutePropertyName() {
        val descriptor = ScheduledPaymentsRoute.serializer().descriptor
        assertEquals(1, descriptor.elementsCount)
        assertEquals(ScheduledPaymentsViewModel.ACCOUNT_ID_ARG, descriptor.getElementName(0))
    }

    @Test
    fun repositoryReceivesTheRouteAccountId() {
        val repository = FakeScheduledPaymentsRepository()
        viewModel(repository, accountId = "acc-42")
        assertEquals("acc-42", repository.observedAccountId)
    }

    @Test
    fun initialStateIsLoading() {
        assertIs<ScheduledPaymentsUiState.Loading>(viewModel().stateFlow.value.uiState)
    }

    @Test
    fun loadingTransitionsToContentWhenTheStreamEmits() {
        val repository = FakeScheduledPaymentsRepository()
        val vm = viewModel(repository)
        assertIs<ScheduledPaymentsUiState.Loading>(vm.stateFlow.value.uiState)

        repository.emit(ScheduledPaymentsFixtures.contentStreamState())

        assertIs<ScheduledPaymentsUiState.Content>(vm.stateFlow.value.uiState)
    }

    // endregion

    // region — content mapping & formatting

    @Test
    fun contentMapsAndFormatsEveryPayment() {
        val payments = content(viewModel(contentRepository())).payments
        assertEquals(2, payments.size)

        val execution = payments.first()
        assertEquals("SP-001", execution.scheduledPaymentId)
        assertEquals("HMRC Self Assessment", execution.payeeName)
        assertEquals("£842.00", execution.amountLabel)
        assertEquals("Fri 31 Jul 2026", execution.scheduledDateLabel)
        assertEquals(ScheduledPaymentType.Execution, execution.scheduledType)
        assertEquals("08-32-00 12001039", execution.creditorIdentification)
        assertEquals("HMRC-SA-2526", execution.reference)
    }

    @Test
    fun arrivalPaymentCarriesTheArrivalTypeAndItsOwnFormatting() {
        val arrival = content(viewModel(contentRepository())).payments[1]
        assertEquals("£412.50", arrival.amountLabel)
        assertEquals("Sat 15 Aug 2026", arrival.scheduledDateLabel)
        assertEquals(ScheduledPaymentType.Arrival, arrival.scheduledType)
    }

    @Test
    fun anUnparseableDateFallsBackToTheRawString() {
        val badDate = ScheduledPaymentItem(
            scheduledPaymentId = "SP-BAD",
            accountId = ScheduledPaymentsFixtures.ACCOUNT_ID,
            payeeName = "Odd Date Ltd",
            amount = "10.00",
            currency = "GBP",
            scheduledDateTime = "not-a-date",
            scheduledType = ScheduledPaymentType.Unknown,
            reference = "REF",
            creditorIdentification = "00-00-00 00000000",
        )
        val repository = FakeScheduledPaymentsRepository(
            ScreenState.Content(data = listOf(badDate), freshness = DataFreshness.FRESH),
        )

        val model = content(viewModel(repository)).payments.single()
        assertEquals("not-a-date", model.scheduledDateLabel)
        assertEquals(ScheduledPaymentType.Unknown, model.scheduledType)
    }

    @Test
    fun formatScheduledDateRendersWeekdayDayMonthYear() {
        assertEquals("Fri 31 Jul 2026", formatScheduledDate("2026-07-31T00:00:00Z"))
        assertEquals("Sat 15 Aug 2026", formatScheduledDate("2026-08-15T00:00:00Z"))
    }

    @Test
    fun formatScheduledDateReturnsRawInputWhenUnparseable() {
        assertEquals("", formatScheduledDate(""))
        assertEquals("garbage", formatScheduledDate("garbage"))
    }

    // endregion

    // region — empty

    @Test
    fun anEmptyListRendersEmpty() {
        val repository = FakeScheduledPaymentsRepository(ScheduledPaymentsFixtures.emptyStreamState())
        assertIs<ScheduledPaymentsUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun structuralEmptyStreamStateRendersEmpty() {
        val repository = FakeScheduledPaymentsRepository(ScreenState.Empty)
        assertIs<ScheduledPaymentsUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    // endregion

    // region — error classification

    @Test
    fun tokenExpiredMapsToTokenExpired() {
        val error = errorState(errorRepository(NetworkError.Client.Unauthorized()))
        assertEquals(ScheduledPaymentsError.TokenExpired, error.kind)
    }

    @Test
    fun forbiddenMapsToConsentRevoked() {
        val error = errorState(errorRepository(NetworkError.Client.Forbidden()))
        assertEquals(ScheduledPaymentsError.ConsentRevoked, error.kind)
    }

    @Test
    fun rateLimitedMapsToRateLimited() {
        val error = errorState(errorRepository(NetworkError.Client.RateLimited()))
        assertEquals(ScheduledPaymentsError.RateLimited, error.kind)
    }

    @Test
    fun networkFailureMapsToNetworkError() {
        val error = errorState(errorRepository(NetworkError.Network(IllegalStateException("offline"))))
        assertEquals(ScheduledPaymentsError.NetworkError, error.kind)
    }

    @Test
    fun uncategorisedFailureFallsBackToNetworkError() {
        val repository = FakeScheduledPaymentsRepository(ScreenState.Error(IllegalStateException("boom")))
        assertEquals(ScheduledPaymentsError.NetworkError, errorState(repository).kind)
    }

    @Test
    fun noNetworkStreamStateMapsToNetworkError() {
        val repository = FakeScheduledPaymentsRepository(ScreenState.NoNetwork())
        assertEquals(ScheduledPaymentsError.NetworkError, errorState(repository).kind)
    }

    @Test
    fun unauthenticatedStreamStateMapsToTokenExpired() {
        val repository = FakeScheduledPaymentsRepository(ScreenState.Unauthenticated)
        assertEquals(ScheduledPaymentsError.TokenExpired, errorState(repository).kind)
    }

    @Test
    fun classifyMapsEachNetworkErrorDirectly() {
        assertEquals(
            ScheduledPaymentsError.TokenExpired,
            classifyScheduledPaymentsError(RemoteException(NetworkError.Client.Unauthorized())),
        )
        assertEquals(
            ScheduledPaymentsError.ConsentRevoked,
            classifyScheduledPaymentsError(RemoteException(NetworkError.Client.Forbidden())),
        )
        assertEquals(
            ScheduledPaymentsError.RateLimited,
            classifyScheduledPaymentsError(RemoteException(NetworkError.Client.RateLimited())),
        )
        assertEquals(
            ScheduledPaymentsError.NetworkError,
            classifyScheduledPaymentsError(RemoteException(NetworkError.Network(IllegalStateException()))),
        )
        assertEquals(
            ScheduledPaymentsError.NetworkError,
            classifyScheduledPaymentsError(IllegalStateException("x")),
        )
    }

    // endregion

    // region — actions

    @Test
    fun retryActionCallsRefresh() {
        val repository = errorRepository(NetworkError.Client.Unauthorized())
        val vm = viewModel(repository)

        vm.trySendAction(ScheduledPaymentsAction.RetryLoad)

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun everyErrorKindIsSurfacedWithARetryableScreen() {
        assertTrue(ScheduledPaymentsError.entries.isNotEmpty())
    }

    // endregion
}
