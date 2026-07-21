/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.DirectDebitItem
import org.mifosx.openbanking.core.model.banking.DirectDebitsSummary
import org.mifosx.openbanking.feature.directdebits.DirectDebitsFixtures
import org.mifosx.openbanking.feature.directdebits.FakeDirectDebitsRepository
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers [DirectDebitsViewModel]'s state mapping, display formatting and error classification.
 *
 * Test names are camelCase, not backticked: this source set also compiles for Kotlin/Native, whose
 * frontend rejects the parentheses and commas a prose-style backticked name would carry.
 */
class DirectDebitsViewModelTest {

    private fun viewModel(
        repository: FakeDirectDebitsRepository = FakeDirectDebitsRepository(),
        accountId: String = DirectDebitsFixtures.ACCOUNT_ID,
    ): DirectDebitsViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return DirectDebitsViewModel(
            savedStateHandle = SavedStateHandle(mapOf("accountId" to accountId)),
            repository = repository,
        )
    }

    private fun content(summary: DirectDebitsSummary): ScreenState<DirectDebitsSummary> =
        ScreenState.Content(data = summary, freshness = DataFreshness.FRESH)

    private fun remoteFailure(error: NetworkError): ScreenState<DirectDebitsSummary> =
        ScreenState.Error(RemoteException(error))

    @Test
    fun accountIdIsReadFromSavedStateHandleUnderTheRoutePropertyName() {
        val vm = viewModel(accountId = "acc-77")
        assertEquals("acc-77", vm.stateFlow.value.accountId)
    }

    @Test
    fun missingAccountIdFallsBackToEmptyRatherThanCrashing() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val vm = DirectDebitsViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeDirectDebitsRepository(),
        )
        assertEquals("", vm.stateFlow.value.accountId)
    }

    @Test
    fun accountIdArgConstantMatchesTheRoutePropertyName() {
        assertEquals("accountId", DirectDebitsViewModel.ACCOUNT_ID_ARG)
    }

    @Test
    fun initialStateIsLoading() {
        val vm = viewModel()
        assertIs<DirectDebitsUiState.Loading>(vm.stateFlow.value.uiState)
    }

    @Test
    fun repositoryReceivesTheRouteAccountId() = runTest {
        val repository = FakeDirectDebitsRepository()
        viewModel(repository, accountId = "acc-9")
        assertEquals("acc-9", repository.observedAccountId)
    }

    @Test
    fun contentStateExposesRowsAndSummaryCounts() {
        val repository = FakeDirectDebitsRepository(content(DirectDebitsFixtures.summary()))
        val state = viewModel(repository).stateFlow.value.uiState

        val rendered = assertIs<DirectDebitsUiState.Content>(state)
        assertEquals(4, rendered.mandates.size)
        assertEquals(3, rendered.activeCount)
        assertEquals(1, rendered.inactiveCount)
    }

    @Test
    fun contentPreservesActiveFirstOrderFromTheMapper() {
        val repository = FakeDirectDebitsRepository(content(DirectDebitsFixtures.summary()))
        val rendered = assertIs<DirectDebitsUiState.Content>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(
            listOf("British Gas", "Vodafone", "Aviva Insurance", "TV Licensing"),
            rendered.mandates.map { it.name },
        )
        assertFalse(rendered.mandates.last().isActive)
    }

    @Test
    fun amountsAreFormattedWithCurrencySymbolAndDatesAsDayMonthYear() {
        val repository = FakeDirectDebitsRepository(content(DirectDebitsFixtures.summary()))
        val rendered = assertIs<DirectDebitsUiState.Content>(viewModel(repository).stateFlow.value.uiState)

        val first = rendered.mandates.first()
        assertEquals("£78.00", first.amountLabel)
        assertEquals("15 Jun 2026", first.lastCollectedLabel)
        assertEquals("DD-BG-44120", first.mandateId)
        assertEquals("Active", first.statusLabel)
    }

    @Test
    fun inactiveMandateKeepsItsStatusLabelAndIsMarkedInactive() {
        val repository = FakeDirectDebitsRepository(content(DirectDebitsFixtures.summary()))
        val rendered = assertIs<DirectDebitsUiState.Content>(viewModel(repository).stateFlow.value.uiState)

        val inactive = rendered.mandates.last()
        assertEquals("Inactive", inactive.statusLabel)
        assertFalse(inactive.isActive)
        assertEquals("£13.25", inactive.amountLabel)
    }

    @Test
    fun mandateWithNoAmountOrDateRendersBlankLabelsRatherThanPlaceholders() {
        val bare = DirectDebitItem(
            mandateId = "",
            name = "Unknown Originator",
            statusCode = "",
            isActive = false,
            previousPaymentAmount = "",
            currency = "",
            previousPaymentDateTime = "",
        )
        val repository = FakeDirectDebitsRepository(
            content(DirectDebitsSummary(items = listOf(bare), activeCount = 0, inactiveCount = 1)),
        )
        val rendered = assertIs<DirectDebitsUiState.Content>(viewModel(repository).stateFlow.value.uiState)

        val row = rendered.mandates.single()
        assertEquals("", row.amountLabel)
        assertEquals("", row.lastCollectedLabel)
        assertEquals("", row.mandateId)
        assertEquals("Unknown Originator", row.name)
    }

    @Test
    fun contentCarryingNoMandatesRendersTheEmptyState() {
        val repository = FakeDirectDebitsRepository(content(DirectDebitsFixtures.emptySummary()))
        assertIs<DirectDebitsUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun emptyStreamStateRendersTheEmptyState() {
        val repository = FakeDirectDebitsRepository(ScreenState.Empty)
        assertIs<DirectDebitsUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun unauthorizedMapsToRetriableTokenExpired() {
        val repository = FakeDirectDebitsRepository(remoteFailure(NetworkError.Client.Unauthorized()))
        val error = assertIs<DirectDebitsUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(DirectDebitsErrorKind.TokenExpired, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun forbiddenMapsToConsentRevokedAndIsNotRetriable() {
        val repository = FakeDirectDebitsRepository(remoteFailure(NetworkError.Client.Forbidden()))
        val error = assertIs<DirectDebitsUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(DirectDebitsErrorKind.ConsentRevoked, error.kind)
        assertFalse(error.kind.isRetriable)
    }

    @Test
    fun rateLimitedMapsToRetriableRateLimited() {
        val repository = FakeDirectDebitsRepository(remoteFailure(NetworkError.Client.RateLimited()))
        val error = assertIs<DirectDebitsUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(DirectDebitsErrorKind.RateLimited, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun transportNetworkFailureMapsToRetriableNetworkError() {
        val repository = FakeDirectDebitsRepository(
            remoteFailure(NetworkError.Network(IllegalStateException("offline"))),
        )
        val error = assertIs<DirectDebitsUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(DirectDebitsErrorKind.NetworkError, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun uncategorisedFailureFallsBackToRetriableServerError() {
        val repository = FakeDirectDebitsRepository(ScreenState.Error(IllegalStateException("boom")))
        val error = assertIs<DirectDebitsUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(DirectDebitsErrorKind.ServerError, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun serverFailureMapsToRetriableServerError() {
        val repository = FakeDirectDebitsRepository(
            remoteFailure(NetworkError.Server(statusCode = SERVER_ERROR_STATUS)),
        )
        val error = assertIs<DirectDebitsUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(DirectDebitsErrorKind.ServerError, error.kind)
    }

    @Test
    fun noNetworkStreamStateMapsToNetworkError() {
        val repository = FakeDirectDebitsRepository(ScreenState.NoNetwork())
        val error = assertIs<DirectDebitsUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(DirectDebitsErrorKind.NetworkError, error.kind)
    }

    @Test
    fun unauthenticatedStreamStateMapsToTokenExpired() {
        val repository = FakeDirectDebitsRepository(ScreenState.Unauthenticated)
        val error = assertIs<DirectDebitsUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(DirectDebitsErrorKind.TokenExpired, error.kind)
    }

    @Test
    fun retryActionRefreshesTheRepository() {
        val repository = FakeDirectDebitsRepository(remoteFailure(NetworkError.Client.Unauthorized()))
        val vm = viewModel(repository)

        vm.trySendAction(DirectDebitsAction.RetryLoad)

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun streamTransitionsFromLoadingToContentAsTheFetchLands() {
        val repository = FakeDirectDebitsRepository()
        val vm = viewModel(repository)
        assertIs<DirectDebitsUiState.Loading>(vm.stateFlow.value.uiState)

        repository.emit(content(DirectDebitsFixtures.summary()))

        assertIs<DirectDebitsUiState.Content>(vm.stateFlow.value.uiState)
    }

    @Test
    fun everyErrorKindExceptConsentRevokedIsRetriable() {
        val notRetriable = DirectDebitsErrorKind.entries.filterNot { it.isRetriable }
        assertEquals(listOf(DirectDebitsErrorKind.ConsentRevoked), notRetriable)
    }

    @Test
    fun mandateDateFormatterTrimsLeadingZeroesFromTheDay() {
        assertEquals("5 Jun 2026", formatMandateDate("2026-06-05T00:00:00Z"))
        assertEquals("1 Mar 2026", formatMandateDate("2026-03-01T00:00:00Z"))
    }

    @Test
    fun mandateDateFormatterHandlesDateOnlyInput() {
        assertEquals("20 Dec 2026", formatMandateDate("2026-12-20"))
    }

    @Test
    fun mandateDateFormatterReturnsUnparseableInputUnchanged() {
        assertEquals("not-a-date", formatMandateDate("not-a-date"))
        assertEquals("2026-13-01", formatMandateDate("2026-13-01"))
        assertEquals("", formatMandateDate(""))
    }

    private companion object {
        const val SERVER_ERROR_STATUS = 500
    }
}
