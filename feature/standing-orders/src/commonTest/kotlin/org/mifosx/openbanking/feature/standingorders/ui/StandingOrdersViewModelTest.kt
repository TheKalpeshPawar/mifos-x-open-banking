/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.StandingOrderItem
import org.mifosx.openbanking.core.model.banking.StandingOrdersSummary
import org.mifosx.openbanking.feature.standingorders.FakeStandingOrdersRepository
import org.mifosx.openbanking.feature.standingorders.StandingOrdersFixtures
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers [StandingOrdersViewModel]'s state mapping, display formatting and error classification.
 *
 * Test names are camelCase, not backticked: this source set also compiles for Kotlin/Native, whose
 * frontend rejects the parentheses and commas a prose-style backticked name would carry.
 */
class StandingOrdersViewModelTest {

    private fun viewModel(
        repository: FakeStandingOrdersRepository = FakeStandingOrdersRepository(),
        accountId: String = StandingOrdersFixtures.ACCOUNT_ID,
    ): StandingOrdersViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return StandingOrdersViewModel(
            savedStateHandle = SavedStateHandle(mapOf("accountId" to accountId)),
            repository = repository,
        )
    }

    private fun content(summary: StandingOrdersSummary): ScreenState<StandingOrdersSummary> =
        ScreenState.Content(data = summary, freshness = DataFreshness.FRESH)

    private fun remoteFailure(error: NetworkError): ScreenState<StandingOrdersSummary> =
        ScreenState.Error(RemoteException(error))

    private fun contentRows(
        repository: FakeStandingOrdersRepository,
    ): List<StandingOrderRowUi> =
        assertIs<StandingOrdersUiState.Content>(viewModel(repository).stateFlow.value.uiState).orders

    @Test
    fun accountIdIsReadFromSavedStateHandleUnderTheRoutePropertyName() {
        val vm = viewModel(accountId = "acc-77")
        assertEquals("acc-77", vm.stateFlow.value.accountId)
    }

    @Test
    fun missingAccountIdFallsBackToEmptyRatherThanCrashing() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val vm = StandingOrdersViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeStandingOrdersRepository(),
        )
        assertEquals("", vm.stateFlow.value.accountId)
    }

    @Test
    fun accountIdArgConstantMatchesTheRoutePropertyName() {
        assertEquals("accountId", StandingOrdersViewModel.ACCOUNT_ID_ARG)
    }

    @Test
    fun initialStateIsLoading() {
        val vm = viewModel()
        assertIs<StandingOrdersUiState.Loading>(vm.stateFlow.value.uiState)
    }

    @Test
    fun repositoryReceivesTheRouteAccountId() = runTest {
        val repository = FakeStandingOrdersRepository()
        viewModel(repository, accountId = "acc-9")
        assertEquals("acc-9", repository.observedAccountId)
    }

    @Test
    fun contentPreservesActiveFirstOrderFromTheMapper() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val rows = contentRows(repository)

        assertEquals(
            listOf("Jameson Lettings", "ISA Saver", "PureGym", "Marcus Savings", "Oxfam GB"),
            rows.map { it.payeeName },
        )
        assertFalse(rows.last().isActive)
    }

    @Test
    fun amountsAreFormattedWithCurrencySymbolAndGroupingSeparator() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val first = contentRows(repository).first()

        assertEquals("£1,200.00", first.amountLabel)
        assertEquals("GBP", first.currencyLabel)
    }

    @Test
    fun datesAreFormattedAsDayMonthYear() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val rows = contentRows(repository)

        assertEquals("1 Jul 2026", rows.first().nextDateLabel)
        assertEquals("15 Jul 2026", rows[PUREGYM_INDEX].nextDateLabel)
    }

    @Test
    fun frequencyLabelReachesTheRowUnchangedFromTheMapper() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val rows = contentRows(repository)

        assertEquals("Monthly on the 1st", rows.first().frequencyLabel)
        assertEquals("Weekly every Friday", rows[MARCUS_INDEX].frequencyLabel)
    }

    @Test
    fun orderWithAFinalPaymentCarriesTheFlagAndTheFormattedDate() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val marcus = contentRows(repository)[MARCUS_INDEX]

        assertTrue(marcus.hasFinalPayment)
        assertEquals("25 Dec 2026", marcus.finalDateLabel)
    }

    @Test
    fun orderWithoutAFinalPaymentCarriesNoFlagAndABlankDate() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val first = contentRows(repository).first()

        assertFalse(first.hasFinalPayment)
        assertEquals("", first.finalDateLabel)
    }

    @Test
    fun inactiveOrderKeepsItsStatusLabelAndIsMarkedInactive() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val inactive = contentRows(repository).last()

        assertEquals("Inactive", inactive.statusLabel)
        assertFalse(inactive.isActive)
        assertEquals("£10.00", inactive.amountLabel)
    }

    @Test
    fun cancelledOrderWithNoNextPaymentRendersABlankNextDateRatherThanAPlaceholder() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val inactive = contentRows(repository).last()

        assertEquals("", inactive.nextDateLabel)
        assertEquals("28 Dec 2025", inactive.finalDateLabel)
    }

    @Test
    fun sortCodeAndReferenceReachTheRowVerbatim() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val first = contentRows(repository).first()

        assertEquals("40-12-09 65872310", first.sortCodeLabel)
        assertEquals("RENT-FLAT12", first.referenceLabel)
        assertEquals("SO-001", first.standingOrderId)
    }

    @Test
    fun orderWithNoAmountOrDatesRendersBlankLabelsRatherThanPlaceholders() {
        val bare = StandingOrderItem(
            standingOrderId = "",
            payeeName = "Unknown Payee",
            statusCode = "",
            isActive = false,
            nextPaymentAmount = "",
            currency = "",
            frequencyLabel = "",
            nextPaymentDateTime = "",
            finalPaymentDateTime = "",
            hasFinalPayment = false,
            creditorIdentification = "",
            reference = "",
        )
        val repository = FakeStandingOrdersRepository(
            content(StandingOrdersSummary(items = listOf(bare), activeCount = 0, inactiveCount = 1)),
        )
        val row = contentRows(repository).single()

        assertEquals("", row.amountLabel)
        assertEquals("", row.nextDateLabel)
        assertEquals("", row.sortCodeLabel)
        assertEquals("Unknown Payee", row.payeeName)
    }

    @Test
    fun contentCarryingNoOrdersRendersTheEmptyState() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.emptySummary()))
        assertIs<StandingOrdersUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun emptyStreamStateRendersTheEmptyState() {
        val repository = FakeStandingOrdersRepository(ScreenState.Empty)
        assertIs<StandingOrdersUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun unauthorizedMapsToTokenExpired() {
        val repository = FakeStandingOrdersRepository(remoteFailure(NetworkError.Client.Unauthorized()))
        val error = assertIs<StandingOrdersUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(StandingOrdersErrorKind.TokenExpired, error.kind)
    }

    @Test
    fun forbiddenMapsToConsentRevoked() {
        val repository = FakeStandingOrdersRepository(remoteFailure(NetworkError.Client.Forbidden()))
        val error = assertIs<StandingOrdersUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(StandingOrdersErrorKind.ConsentRevoked, error.kind)
    }

    /**
     * A U000 refusal is not a failure — the product simply has no standing orders. Routing it to an
     * error kind would offer a Retry that can never succeed.
     */
    @Test
    fun aU000BadRequestRendersUnsupportedRatherThanAnError() {
        val repository = FakeStandingOrdersRepository(
            remoteFailure(NetworkError.Client.BadRequest(body = U000_BODY)),
        )

        assertIs<StandingOrdersUiState.Unsupported>(viewModel(repository).stateFlow.value.uiState)
    }

    /** The bank's own wording is shown, not copy authored here. */
    @Test
    fun theUnsupportedStateCarriesTheBanksOwnMessage() {
        val repository = FakeStandingOrdersRepository(
            remoteFailure(NetworkError.Client.BadRequest(body = U000_BODY)),
        )
        val state = assertIs<StandingOrdersUiState.Unsupported>(
            viewModel(repository).stateFlow.value.uiState,
        )

        assertEquals("This action is not allowed on the account type in the request", state.message)
    }

    /** A 400 for any other reason stays an error, so its Retry survives. */
    @Test
    fun aBadRequestWithoutU000StaysAnError() {
        val repository = FakeStandingOrdersRepository(
            remoteFailure(NetworkError.Client.BadRequest(body = """{"Code":"400"}""")),
        )

        assertIs<StandingOrdersUiState.Error>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun rateLimitedMapsToRateLimited() {
        val repository = FakeStandingOrdersRepository(remoteFailure(NetworkError.Client.RateLimited()))
        val error = assertIs<StandingOrdersUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(StandingOrdersErrorKind.RateLimited, error.kind)
    }

    @Test
    fun transportNetworkFailureMapsToNetworkError() {
        val repository = FakeStandingOrdersRepository(
            remoteFailure(NetworkError.Network(IllegalStateException("offline"))),
        )
        val error = assertIs<StandingOrdersUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(StandingOrdersErrorKind.NetworkError, error.kind)
    }

    @Test
    fun uncategorisedFailureFallsBackToServerError() {
        val repository = FakeStandingOrdersRepository(ScreenState.Error(IllegalStateException("boom")))
        val error = assertIs<StandingOrdersUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(StandingOrdersErrorKind.ServerError, error.kind)
    }

    @Test
    fun serverFailureMapsToServerError() {
        val repository = FakeStandingOrdersRepository(
            remoteFailure(NetworkError.Server(statusCode = SERVER_ERROR_STATUS)),
        )
        val error = assertIs<StandingOrdersUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(StandingOrdersErrorKind.ServerError, error.kind)
    }

    @Test
    fun noNetworkStreamStateMapsToNetworkError() {
        val repository = FakeStandingOrdersRepository(ScreenState.NoNetwork())
        val error = assertIs<StandingOrdersUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(StandingOrdersErrorKind.NetworkError, error.kind)
    }

    @Test
    fun unauthenticatedStreamStateMapsToTokenExpired() {
        val repository = FakeStandingOrdersRepository(ScreenState.Unauthenticated)
        val error = assertIs<StandingOrdersUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(StandingOrdersErrorKind.TokenExpired, error.kind)
    }

    @Test
    fun retryActionRefreshesTheRepository() {
        val repository = FakeStandingOrdersRepository(remoteFailure(NetworkError.Client.Unauthorized()))
        val vm = viewModel(repository)

        vm.trySendAction(StandingOrdersAction.RetryLoad)

        assertEquals(1, repository.refreshCount)
    }

    /**
     * Pull-to-refresh dispatches the same action the Retry button does, so a second dispatch from
     * content — where the gesture lives — must reach the repository just as the first did.
     */
    @Test
    fun retryActionFromContentRefreshesAgainSoPullToRefreshRepeats() {
        val repository = FakeStandingOrdersRepository(content(StandingOrdersFixtures.summary()))
        val vm = viewModel(repository)

        vm.trySendAction(StandingOrdersAction.RetryLoad)
        vm.trySendAction(StandingOrdersAction.RetryLoad)

        assertEquals(2, repository.refreshCount)
    }

    @Test
    fun streamTransitionsFromLoadingToContentAsTheFetchLands() {
        val repository = FakeStandingOrdersRepository()
        val vm = viewModel(repository)
        assertIs<StandingOrdersUiState.Loading>(vm.stateFlow.value.uiState)

        repository.emit(content(StandingOrdersFixtures.summary()))

        assertIs<StandingOrdersUiState.Content>(vm.stateFlow.value.uiState)
    }

    @Test
    fun standingOrderDateFormatterTrimsLeadingZeroesFromTheDay() {
        assertEquals("4 Jul 2026", formatStandingOrderDate("2026-07-04T00:00:00Z"))
        assertEquals("1 Jan 2026", formatStandingOrderDate("2026-01-01T00:00:00Z"))
    }

    @Test
    fun standingOrderDateFormatterHandlesDateOnlyInput() {
        assertEquals("25 Dec 2026", formatStandingOrderDate("2026-12-25"))
    }

    @Test
    fun standingOrderDateFormatterReturnsUnparseableInputUnchanged() {
        assertEquals("not-a-date", formatStandingOrderDate("not-a-date"))
        assertEquals("2026-13-01", formatStandingOrderDate("2026-13-01"))
        assertEquals("", formatStandingOrderDate(""))
    }

    private companion object {
        const val SERVER_ERROR_STATUS = 500
        const val PUREGYM_INDEX = 2
        const val MARCUS_INDEX = 3

        /** Captured from the HSBC sandbox verbatim. */
        const val U000_BODY = """
            {"Code":"400","Id":"842f0682-ba4a-4f17-9107-a5ce8b98fdbd","Message":"Bad Request",
             "Errors":[{"ErrorCode":"U000",
                        "Message":"This action is not allowed on the account type in the request"}]}
        """
    }
}
