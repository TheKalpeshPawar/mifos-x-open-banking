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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.feature.transactions.ui.TransactionFilter
import org.mifosx.openbanking.feature.transactions.ui.TransactionGroup
import org.mifosx.openbanking.feature.transactions.ui.TransactionRowUi
import org.mifosx.openbanking.feature.transactions.ui.TransactionsAction
import org.mifosx.openbanking.feature.transactions.ui.TransactionsData
import org.mifosx.openbanking.feature.transactions.ui.TransactionsState
import org.mifosx.openbanking.feature.transactions.ui.TransactionsUiState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// androidInstrumentedTest does not see commonTest, so the display fixture is inlined here.
private fun contentState(
    hasNextPage: Boolean = false,
    isPaginating: Boolean = false,
): TransactionsState = TransactionsState(
    accountId = "acc-1",
    uiState = TransactionsUiState.Content(
        TransactionsData(
            groups = listOf(
                TransactionGroup(
                    dateLabel = "SAT 27 JUN 2026",
                    rows = listOf(
                        TransactionRowUi(
                            key = "t1",
                            transactionId = "t1",
                            description = "TESCO STORES",
                            amountLabel = "- £42.17",
                            isCredit = false,
                            category = TransactionCategory.GROCERIES,
                            isPending = false,
                        ),
                        TransactionRowUi(
                            key = "t2",
                            transactionId = "t2",
                            description = "SPOTIFY AB",
                            amountLabel = "- £11.99",
                            isCredit = false,
                            category = TransactionCategory.SUBSCRIPTIONS,
                            isPending = true,
                        ),
                    ),
                ),
            ),
            moneyInLabel = "+£0.00",
            moneyOutLabel = "-£54.16",
        ),
    ),
    hasNextPage = hasNextPage,
    isPaginating = isPaginating,
)

/**
 * On-device mirror of [TransactionsScreenRobolectricTest]; runs under
 * `:feature:transactions:connectedDemoDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class TransactionsScreenInstrumentedTest {

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
    fun contentStateRendersSummaryChipsSearchAndRows() {
        render(contentState())
        composeRule.onNodeWithTag(TransactionsTestTags.PERIOD_SUMMARY).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.FILTER_ROW).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.SEARCH_FIELD).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.LIST).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.row("t1")).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.pendingBadge("t2"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun loadMoreButtonVisibleWhenAnotherPageExistsAndDispatches() {
        render(contentState(hasNextPage = true))
        composeRule.onNodeWithTag(TransactionsTestTags.LIST)
            .performScrollToNode(hasTestTag(TransactionsTestTags.LOAD_MORE))
        composeRule.onNodeWithTag(TransactionsTestTags.LOAD_MORE).performClick()
        assertTrue(TransactionsAction.LoadMore in actions)
    }

    @Test
    fun paginationLoaderReplacesLoadMoreWhileLoading() {
        render(contentState(hasNextPage = true, isPaginating = true))
        composeRule.onNodeWithTag(TransactionsTestTags.LIST)
            .performScrollToNode(hasTestTag(TransactionsTestTags.PAGINATION_LOADER))
        composeRule.onNodeWithTag(TransactionsTestTags.PAGINATION_LOADER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.LOAD_MORE).assertDoesNotExist()
    }

    @Test
    fun filterChipClickDispatchesFilterAction() {
        render(contentState())
        composeRule.onNodeWithTag(TransactionsTestTags.filterChip(TransactionFilter.MONEY_OUT)).performClick()
        assertTrue(TransactionsAction.FilterTransactions(TransactionFilter.MONEY_OUT) in actions)
    }

    @Test
    fun typingInSearchDispatchesSearchAction() {
        render(contentState())
        composeRule.onNodeWithTag(TransactionsTestTags.SEARCH_FIELD).performTextInput("tesco")
        assertTrue(actions.any { it is TransactionsAction.SearchTransactions })
    }

    @Test
    fun rowClickInvokesRowCallbackWithTransactionId() {
        var clicked: String? = null
        render(contentState(), onRowClick = { clicked = it })
        composeRule.onNodeWithTag(TransactionsTestTags.row("t1")).performClick()
        assertEquals("t1", clicked)
    }

    @Test
    fun emptyStateKeepsFiltersAndClearsThem() {
        render(TransactionsState(uiState = TransactionsUiState.Empty, activeFilter = TransactionFilter.MONEY_OUT))
        composeRule.onNodeWithTag(TransactionsTestTags.EMPTY).assertExists()
        composeRule.onNodeWithTag(TransactionsTestTags.CLEAR_FILTERS).performClick()
        assertTrue(TransactionsAction.ClearFilters in actions)
    }
}
