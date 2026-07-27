/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.transactions.ui.TransactionFilter
import org.mifosx.openbanking.feature.transactions.ui.TransactionsAction
import org.mifosx.openbanking.feature.transactions.ui.TransactionsErrorKind
import org.mifosx.openbanking.feature.transactions.ui.TransactionsState
import org.mifosx.openbanking.feature.transactions.ui.TransactionsUiState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [TransactionsScreenContent] across its states under Robolectric (JVM, no device) and drives
 * it through the shared [TransactionsTestTags]. A verbatim on-device mirror lives in
 * [TransactionsScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class TransactionsScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<TransactionsAction>()

    private fun render(
        state: TransactionsState,
        onRowClick: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            TransactionsScreenContent(state = state, onAction = { actions.add(it) }, onRowClick = onRowClick)
        }
    }

    @Test
    fun loadingStateRendersSkeleton() {
        render(TransactionsState(uiState = TransactionsUiState.Loading))
        composeRule.onNodeWithTag(TransactionsTestTags.SKELETON).assertExists()
    }

    @Test
    fun contentStateRendersSummaryChipsSearchAndRows() {
        render(TransactionsFixtures.contentState())
        composeRule.onNodeWithTag(TransactionsTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.PERIOD_SUMMARY).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.FILTER_ROW).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.SEARCH_FIELD).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.LIST).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.MONEY_IN, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.MONEY_OUT, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.row("t1")).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.pendingBadge("t2"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun loadMoreButtonVisibleWhenAnotherPageExistsAndDispatches() {
        render(TransactionsFixtures.contentState(hasNextPage = true))
        composeRule.onNodeWithTag(TransactionsTestTags.LIST)
            .performScrollToNode(hasTestTag(TransactionsTestTags.LOAD_MORE))
        composeRule.onNodeWithTag(TransactionsTestTags.LOAD_MORE).performClick()
        assertTrue(TransactionsAction.LoadMore in actions)
    }

    @Test
    fun paginationLoaderReplacesLoadMoreWhileLoading() {
        render(TransactionsFixtures.contentState(hasNextPage = true, isPaginating = true))
        composeRule.onNodeWithTag(TransactionsTestTags.LIST)
            .performScrollToNode(hasTestTag(TransactionsTestTags.PAGINATION_LOADER))
        composeRule.onNodeWithTag(TransactionsTestTags.PAGINATION_LOADER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.LOAD_MORE).assertDoesNotExist()
    }

    @Test
    fun filterChipClickDispatchesFilterAction() {
        render(TransactionsFixtures.contentState())
        composeRule.onNodeWithTag(TransactionsTestTags.filterChip(TransactionFilter.MONEY_OUT)).performClick()
        assertTrue(TransactionsAction.FilterTransactions(TransactionFilter.MONEY_OUT) in actions)
    }

    @Test
    fun dateRangeChipOpensThePicker() {
        render(TransactionsFixtures.contentState())
        composeRule.onNodeWithTag(TransactionsTestTags.FILTER_DATE_RANGE).performClick()
        assertTrue(TransactionsAction.OpenDateRangePicker in actions)
    }

    @Test
    fun typingInSearchDispatchesSearchAction() {
        render(TransactionsFixtures.contentState())
        composeRule.onNodeWithTag(TransactionsTestTags.SEARCH_FIELD).performTextInput("tesco")
        assertTrue(actions.any { it is TransactionsAction.SearchTransactions })
    }

    @Test
    fun rowClickInvokesRowCallbackWithTransactionId() {
        var clicked: String? = null
        render(TransactionsFixtures.contentState(), onRowClick = { clicked = it })
        composeRule.onNodeWithTag(TransactionsTestTags.row("t1")).performClick()
        assertEquals("t1", clicked)
    }

    @Test
    fun emptyStateKeepsFiltersAndClearsThem() {
        render(TransactionsState(uiState = TransactionsUiState.Empty, activeFilter = TransactionFilter.MONEY_OUT))
        composeRule.onNodeWithTag(TransactionsTestTags.FILTER_ROW).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.SEARCH_FIELD).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.EMPTY).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.CLEAR_FILTERS).performClick()
        assertTrue(TransactionsAction.ClearFilters in actions)
    }

    @Test
    fun recoverableErrorShowsStatusAndRetry() {
        render(TransactionsState(uiState = TransactionsUiState.Error(TransactionsErrorKind.RATE_LIMITED)))
        composeRule.onNodeWithTag(TransactionsTestTags.ERROR).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.ERROR_STATUS, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.ERROR_RETRY).performClick()
        assertTrue(TransactionsAction.RetryLoad in actions)
    }

    @Test
    fun nonRecoverableErrorHidesRetry() {
        render(TransactionsState(uiState = TransactionsUiState.Error(TransactionsErrorKind.CONSENT_WITHDRAWN)))
        composeRule.onNodeWithTag(TransactionsTestTags.ERROR).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.ERROR_RETRY).assertDoesNotExist()
    }
}
