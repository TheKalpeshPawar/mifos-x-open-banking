/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.core.model.banking.TransactionListItem
import org.mifosx.openbanking.core.model.banking.TransactionsPage
import org.mifosx.openbanking.feature.transactions.FakeTransactionsRepository
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TransactionsViewModelTest {

    private val repo = FakeTransactionsRepository()

    private fun item(
        id: String,
        description: String,
        bookingDateTime: String,
        amount: String,
        isCredit: Boolean,
        category: TransactionCategory = TransactionCategory.OTHER,
        pending: Boolean = false,
    ) = TransactionListItem(
        transactionId = id,
        accountId = "acc-1",
        description = description,
        bookingDateTime = bookingDateTime,
        amount = amount,
        currency = "GBP",
        isCredit = isCredit,
        category = category,
        isPending = pending,
    )

    private val spotify = item(
        "t1",
        "SPOTIFY AB",
        "2026-06-28T20:00:00Z",
        "11.99",
        false,
        TransactionCategory.SUBSCRIPTIONS,
        pending = true,
    )
    private val costa = item("t2", "COSTA COFFEE", "2026-06-28T09:00:00Z", "3.65", false, TransactionCategory.DINING)
    private val salary = item("t3", "SALARY ACME LTD", "2026-06-27T06:00:00Z", "2400.00", true)
    private val tesco = item(
        "t4",
        "TESCO STORES",
        "2026-06-27T18:00:00Z",
        "42.17",
        false,
        TransactionCategory.GROCERIES,
    )

    private fun page(items: List<TransactionListItem>, next: String? = null) =
        TransactionsPage(items, nextLink = next, totalPages = null)

    private val fullPage = page(listOf(spotify, costa, salary, tesco))

    private fun viewModel(accountId: String = "acc-1"): TransactionsViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return TransactionsViewModel(SavedStateHandle(mapOf("accountId" to accountId)), repo)
    }

    private fun TransactionsState.content(): TransactionsData =
        assertIs<TransactionsUiState.Content>(uiState).data

    private fun TransactionsData.rowCount(): Int = groups.sumOf { it.rows.size }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun firstPageSuccessRendersDateGroupedContentWithTotals() = runTest {
        repo.firstPageResult = NetworkResult.Success(fullPage)
        val vm = viewModel()

        val state = vm.stateFlow.first { it.uiState is TransactionsUiState.Content }
        val data = state.content()

        assertEquals(2, data.groups.size)
        assertEquals("SUN 28 JUN 2026", data.groups[0].dateLabel)
        assertEquals(2, data.groups[0].rows.size)
        assertEquals("SAT 27 JUN 2026", data.groups[1].dateLabel)
        assertEquals(2, data.groups[1].rows.size)
        assertEquals("+£2,400.00", data.moneyInLabel)
        assertEquals("-£57.81", data.moneyOutLabel)
        assertFalse(state.hasNextPage)

        val spotifyRow = data.groups[0].rows.first { it.transactionId == "t1" }
        assertTrue(spotifyRow.isPending)
        assertEquals("- £11.99", spotifyRow.amountLabel)
        val salaryRow = data.groups[1].rows.first { it.transactionId == "t3" }
        assertTrue(salaryRow.isCredit)
        assertEquals("+ £2,400.00", salaryRow.amountLabel)
    }

    @Test
    fun firstPageWithNoTransactionsRendersEmpty() = runTest {
        repo.firstPageResult = NetworkResult.Success(page(emptyList()))
        val vm = viewModel()

        val state = vm.stateFlow.first { it.uiState !is TransactionsUiState.Loading }
        assertIs<TransactionsUiState.Empty>(state.uiState)
    }

    @Test
    fun rateLimitedFirstPageRendersRecoverableError() = runTest {
        repo.firstPageResult = NetworkResult.Error(NetworkError.Client.RateLimited())
        val vm = viewModel()

        val state = vm.stateFlow.first { it.uiState is TransactionsUiState.Error }
        val error = assertIs<TransactionsUiState.Error>(state.uiState)
        assertEquals(TransactionsErrorKind.RATE_LIMITED, error.kind)
        assertTrue(error.kind.recoverable)
    }

    @Test
    fun forbiddenMapsToNonRecoverableConsentWithdrawn() = runTest {
        repo.firstPageResult = NetworkResult.Error(NetworkError.Client.Forbidden())
        val state = viewModel().stateFlow.first { it.uiState is TransactionsUiState.Error }
        val error = assertIs<TransactionsUiState.Error>(state.uiState)
        assertEquals(TransactionsErrorKind.CONSENT_WITHDRAWN, error.kind)
        assertFalse(error.kind.recoverable)
    }

    @Test
    fun unauthorizedMapsToSessionExpired() = runTest {
        repo.firstPageResult = NetworkResult.Error(NetworkError.Client.Unauthorized())
        val state = viewModel().stateFlow.first { it.uiState is TransactionsUiState.Error }
        assertEquals(TransactionsErrorKind.SESSION_EXPIRED, assertIs<TransactionsUiState.Error>(state.uiState).kind)
    }

    @Test
    fun otherNetworkErrorsMapToNetworkKind() = runTest {
        repo.firstPageResult = NetworkResult.Error(NetworkError.Network(cause = RuntimeException("offline")))
        val state = viewModel().stateFlow.first { it.uiState is TransactionsUiState.Error }
        assertEquals(TransactionsErrorKind.NETWORK, assertIs<TransactionsUiState.Error>(state.uiState).kind)
    }

    @Test
    fun filterMoneyInShowsOnlyCredits() = runTest {
        repo.firstPageResult = NetworkResult.Success(fullPage)
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }

        vm.trySendAction(TransactionsAction.FilterTransactions(TransactionFilter.MONEY_IN))
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals(TransactionFilter.MONEY_IN, state.activeFilter)
        val data = state.content()
        assertEquals(1, data.rowCount())
        assertEquals("+£2,400.00", data.moneyInLabel)
        assertEquals("-£0.00", data.moneyOutLabel)
    }

    @Test
    fun filterMoneyOutShowsOnlyDebits() = runTest {
        repo.firstPageResult = NetworkResult.Success(fullPage)
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }

        vm.trySendAction(TransactionsAction.FilterTransactions(TransactionFilter.MONEY_OUT))
        advanceUntilIdle()

        val data = vm.stateFlow.value.content()
        assertEquals(3, data.rowCount())
        assertEquals("+£0.00", data.moneyInLabel)
        assertEquals("-£57.81", data.moneyOutLabel)
    }

    @Test
    fun searchNarrowsRowsByDescriptionCaseInsensitively() = runTest {
        repo.firstPageResult = NetworkResult.Success(fullPage)
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }

        vm.trySendAction(TransactionsAction.SearchTransactions("tesco"))
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals("tesco", state.query)
        val data = state.content()
        assertEquals(1, data.rowCount())
        assertEquals("TESCO STORES", data.groups.first().rows.first().description)
    }

    @Test
    fun searchWithNoMatchesRendersEmpty() = runTest {
        repo.firstPageResult = NetworkResult.Success(fullPage)
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }

        vm.trySendAction(TransactionsAction.SearchTransactions("no-such-merchant"))
        advanceUntilIdle()

        assertIs<TransactionsUiState.Empty>(vm.stateFlow.value.uiState)
    }

    @Test
    fun clearFiltersRestoresEveryRow() = runTest {
        repo.firstPageResult = NetworkResult.Success(fullPage)
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }
        vm.trySendAction(TransactionsAction.FilterTransactions(TransactionFilter.MONEY_IN))
        vm.trySendAction(TransactionsAction.SearchTransactions("salary"))
        advanceUntilIdle()

        vm.trySendAction(TransactionsAction.ClearFilters)
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals(TransactionFilter.ALL, state.activeFilter)
        assertEquals("", state.query)
        assertEquals(4, state.content().rowCount())
    }

    @Test
    fun setDateRangeFiltersByBookingDate() = runTest {
        repo.firstPageResult = NetworkResult.Success(fullPage)
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }

        val day = LocalDate.parse("2026-06-27")
        vm.trySendAction(TransactionsAction.SetDateRange(day, day))
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertFalse(state.showDateRangePicker)
        val data = state.content()
        assertEquals(1, data.groups.size)
        assertEquals("SAT 27 JUN 2026", data.groups.first().dateLabel)
        assertEquals(2, data.rowCount())
    }

    @Test
    fun openAndDismissDateRangePickerTogglesFlag() = runTest {
        repo.firstPageResult = NetworkResult.Success(fullPage)
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }

        vm.trySendAction(TransactionsAction.OpenDateRangePicker)
        advanceUntilIdle()
        assertTrue(vm.stateFlow.value.showDateRangePicker)

        vm.trySendAction(TransactionsAction.DismissDateRangePicker)
        advanceUntilIdle()
        assertFalse(vm.stateFlow.value.showDateRangePicker)
    }

    @Test
    fun loadMoreAppendsNextPageAndFollowsCursor() = runTest {
        repo.firstPageResult = NetworkResult.Success(page(listOf(salary, tesco), next = "cursor-1"))
        repo.nextPageResults.add(NetworkResult.Success(page(listOf(spotify, costa))))
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }
        assertTrue(vm.stateFlow.value.hasNextPage)

        vm.trySendAction(TransactionsAction.LoadMore)
        advanceUntilIdle()

        assertEquals(1, repo.nextPageCount)
        assertEquals("cursor-1", repo.lastNextLink)
        val state = vm.stateFlow.value
        assertFalse(state.isPaginating)
        assertFalse(state.hasNextPage)
        assertEquals(4, state.content().rowCount())
        assertEquals(2, state.content().groups.size)
    }

    @Test
    fun loadMoreWithoutACursorIsANoOp() = runTest {
        repo.firstPageResult = NetworkResult.Success(fullPage)
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }

        vm.trySendAction(TransactionsAction.LoadMore)
        advanceUntilIdle()

        assertEquals(0, repo.nextPageCount)
    }

    @Test
    fun loadMoreErrorKeepsContentAndClearsPaginating() = runTest {
        repo.firstPageResult = NetworkResult.Success(page(listOf(salary, tesco), next = "cursor-1"))
        repo.nextPageResults.add(NetworkResult.Error(NetworkError.Network(cause = RuntimeException("offline"))))
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }

        vm.trySendAction(TransactionsAction.LoadMore)
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals(1, repo.nextPageCount)
        assertFalse(state.isPaginating)
        assertEquals(2, state.content().rowCount())
    }

    @Test
    fun retryReloadsAfterAnError() = runTest {
        repo.firstPageResult = NetworkResult.Error(NetworkError.Network(cause = RuntimeException("offline")))
        val vm = viewModel()
        vm.stateFlow.first { it.uiState is TransactionsUiState.Error }

        repo.firstPageResult = NetworkResult.Success(fullPage)
        vm.trySendAction(TransactionsAction.RetryLoad)
        advanceUntilIdle()

        vm.stateFlow.first { it.uiState is TransactionsUiState.Content }
        assertEquals(2, repo.firstPageCount)
    }
}
