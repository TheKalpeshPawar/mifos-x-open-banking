/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailAction
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailErrorKind
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailState
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailUiState
import org.mifosx.openbanking.feature.accountdetail.ui.AccountHeaderUi
import org.mifosx.openbanking.feature.accountdetail.ui.BalanceRowUi
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34

private const val ACCOUNT_ID = "acc-1"

private val header = AccountHeaderUi(
    nickname = "Everyday Current",
    accountSubType = "CURRENTACCOUNT",
    identificationLabel = "40-05-15 12345678",
    currency = "GBP",
    servicerIdentification = "MIDLGB2105V",
    lastUpdatedLabel = "28 Jun 2026, 18:30 UTC",
)

private val balances = listOf(
    BalanceRowUi(type = "InterimAvailable", amountLabel = "2,847.63 GBP"),
    BalanceRowUi(type = "InterimBooked", amountLabel = "2,905.10 GBP"),
)

private fun contentState() = AccountDetailState(
    accountId = ACCOUNT_ID,
    uiState = AccountDetailUiState.Content(header = header, balances = balances),
)

private fun emptyState() = AccountDetailState(
    accountId = ACCOUNT_ID,
    uiState = AccountDetailUiState.Empty(header = header),
)

private fun errorState(kind: AccountDetailErrorKind) = AccountDetailState(
    accountId = ACCOUNT_ID,
    uiState = AccountDetailUiState.Error(kind = kind, recoverable = kind.recoverable),
)

/**
 * Renders [AccountDetailScreenContent] across its states under Robolectric (JVM, no device) and
 * drives it through the shared [AccountDetailTestTags]. A verbatim on-device mirror lives in
 * [AccountDetailScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class AccountDetailScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<AccountDetailAction>()
    private val chipClicks = mutableListOf<Pair<AccountDetailChip, String>>()

    private fun render(state: AccountDetailState) {
        composeRule.setContent {
            AccountDetailScreenContent(
                state = state,
                onAction = { actions.add(it) },
                onChipClick = { chip -> chipClicks.add(chip to state.accountId) },
            )
        }
    }

    @Test
    fun loadingStateRendersSpinnerAndSkeleton() {
        render(AccountDetailState(accountId = ACCOUNT_ID))
        composeRule.onNodeWithTag(AccountDetailTestTags.LOADING_SKELETON).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.LOADING_SPINNER, useUnmergedTree = true).assertExists()
    }

    @Test
    fun contentStateRendersHeaderBadgesBalancesAndChips() {
        render(contentState())

        composeRule.onNodeWithTag(AccountDetailTestTags.HEADER_CARD).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.SUBTYPE_LABEL, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.NICKNAME, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.IDENTIFICATION, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.CURRENCY_BADGE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.SERVICER_BADGE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.LAST_UPDATED, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.OPEN_BANKING_BADGE).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.BALANCES_HEADER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.BALANCES_LIST).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.EXPLORE_HEADER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW).assertExists()
    }

    @Test
    fun contentStateRendersOneRowPerBalanceType() {
        render(contentState())
        balances.forEach { row ->
            composeRule.onNodeWithTag(AccountDetailTestTags.balanceRow(row.type), useUnmergedTree = true)
                .assertExists()
        }
        composeRule.onNodeWithTag(AccountDetailTestTags.BALANCES_EMPTY_STATE).assertDoesNotExist()
    }

    @Test
    fun aHeaderWithoutAServicerOrTimestampDropsThoseTwoRowsOnly() {
        render(
            AccountDetailState(
                accountId = ACCOUNT_ID,
                uiState = AccountDetailUiState.Content(
                    header = header.copy(servicerIdentification = "", lastUpdatedLabel = ""),
                    balances = balances,
                ),
            ),
        )

        composeRule.onNodeWithTag(AccountDetailTestTags.HEADER_CARD).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.CURRENCY_BADGE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.SERVICER_BADGE).assertDoesNotExist()
        composeRule.onNodeWithTag(AccountDetailTestTags.LAST_UPDATED).assertDoesNotExist()
    }

    @Test
    fun emptyStateKeepsTheHeaderAndChipsAndDropsTheBalanceList() {
        render(emptyState())

        composeRule.onNodeWithTag(AccountDetailTestTags.HEADER_CARD).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.BALANCES_EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.BALANCES_LIST).assertDoesNotExist()
    }

    @Test
    fun everyExploreChipIsRenderedInDeclarationOrder() {
        render(contentState())
        AccountDetailChip.entries.forEach { chip ->
            composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW)
                .performScrollToNode(hasTestTag(AccountDetailTestTags.chip(chip)))
            composeRule.onNodeWithTag(AccountDetailTestTags.chip(chip)).assertExists()
        }
    }

    @Test
    fun eachChipClickEmitsThatChipWithTheAccountId() {
        render(contentState())
        composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW).performScrollTo()

        AccountDetailChip.entries.forEach { chip ->
            composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW)
                .performScrollToNode(hasTestTag(AccountDetailTestTags.chip(chip)))
            composeRule.onNodeWithTag(AccountDetailTestTags.chip(chip)).performClick()
        }

        assertEquals(AccountDetailChip.entries.map { it to ACCOUNT_ID }, chipClicks)
    }

    @Test
    fun chipsRemainReachableFromTheEmptyState() {
        render(emptyState())
        composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW).performScrollTo()
        composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW)
            .performScrollToNode(hasTestTag(AccountDetailTestTags.chip(AccountDetailChip.Transactions)))
        composeRule.onNodeWithTag(AccountDetailTestTags.chip(AccountDetailChip.Transactions)).performClick()

        assertEquals(listOf(AccountDetailChip.Transactions to ACCOUNT_ID), chipClicks)
    }

    @Test
    fun recoverableErrorShowsTheRetryButtonAndDispatchesRetry() {
        render(errorState(AccountDetailErrorKind.Network))

        composeRule.onNodeWithTag(AccountDetailTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.ERROR_TITLE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.ERROR_BODY, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.RETRY_BUTTON).performClick()

        assertTrue(AccountDetailAction.RetryLoad in actions)
    }

    @Test
    fun consentWithdrawnHidesRetry() {
        render(errorState(AccountDetailErrorKind.ConsentWithdrawn))

        composeRule.onNodeWithTag(AccountDetailTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.ERROR_BODY, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.RETRY_BUTTON).assertDoesNotExist()
    }

    @Test
    fun accountNotFoundHidesRetry() {
        render(errorState(AccountDetailErrorKind.AccountNotFound))

        composeRule.onNodeWithTag(AccountDetailTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(AccountDetailTestTags.RETRY_BUTTON).assertDoesNotExist()
    }
}
