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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailAction
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailErrorKind
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailState
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailUiState
import org.mifosx.openbanking.feature.accountdetail.ui.AccountHeaderUi
import org.mifosx.openbanking.feature.accountdetail.ui.BalanceRowUi
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ACCOUNT_ID = "acc-1"

/** androidInstrumentedTest does not see commonTest, so the display fixtures are inlined here. */
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

/** The two destinations HSBC gates on product type; everything else always renders. */
private val GATED_CHIPS = setOf(AccountDetailChip.StandingOrders, AccountDetailChip.DirectDebits)

private fun contentState(
    availableChips: Set<AccountDetailChip> = AccountDetailChip.entries.toSet(),
) = AccountDetailState(
    accountId = ACCOUNT_ID,
    uiState = AccountDetailUiState.Content(header = header, balances = balances),
    availableChips = availableChips,
)

private fun emptyState(
    availableChips: Set<AccountDetailChip> = AccountDetailChip.entries.toSet(),
) = AccountDetailState(
    accountId = ACCOUNT_ID,
    uiState = AccountDetailUiState.Empty(header = header),
    availableChips = availableChips,
)

private fun errorState(kind: AccountDetailErrorKind) = AccountDetailState(
    accountId = ACCOUNT_ID,
    uiState = AccountDetailUiState.Error(kind = kind, recoverable = kind.recoverable),
)

/**
 * On-device mirror of [AccountDetailScreenRobolectricTest]; runs under
 * `:feature:account-detail:connectedDemoDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AccountDetailScreenInstrumentedTest {

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

    /**
     * An account whose HSBC product serves neither endpoint — a savings account, a credit card or a
     * Global Money wallet. The chips are absent rather than disabled: there is nothing behind them.
     */
    @Test
    fun chipsAbsentFromAvailableChipsAreNotRendered() {
        render(contentState(availableChips = AccountDetailChip.entries.toSet() - GATED_CHIPS))
        composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW).performScrollTo()

        composeRule.onNodeWithTag(AccountDetailTestTags.chip(AccountDetailChip.StandingOrders))
            .assertDoesNotExist()
        composeRule.onNodeWithTag(AccountDetailTestTags.chip(AccountDetailChip.DirectDebits))
            .assertDoesNotExist()
    }

    @Test
    fun theRemainingChipsStillRenderWhenTwoAreHidden() {
        render(contentState(availableChips = AccountDetailChip.entries.toSet() - GATED_CHIPS))
        composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW).performScrollTo()

        (AccountDetailChip.entries - GATED_CHIPS).forEach { chip ->
            composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW)
                .performScrollToNode(hasTestTag(AccountDetailTestTags.chip(chip)))
            composeRule.onNodeWithTag(AccountDetailTestTags.chip(chip)).assertExists()
        }
    }

    /**
     * Hiding must not reorder. `ExploreChipRow` filters `entries` rather than iterating the set
     * precisely because a Set has no guaranteed order — if that is ever inverted, this fails.
     */
    @Test
    fun hidingAChipDoesNotDisturbTheOrderOfTheRest() {
        val remaining = AccountDetailChip.entries - GATED_CHIPS
        render(contentState(availableChips = remaining.toSet()))
        composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW).performScrollTo()

        remaining.forEach { chip ->
            composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW)
                .performScrollToNode(hasTestTag(AccountDetailTestTags.chip(chip)))
            composeRule.onNodeWithTag(AccountDetailTestTags.chip(chip)).performClick()
        }

        assertEquals(remaining.map { it to ACCOUNT_ID }, chipClicks)
    }

    @Test
    fun hiddenChipsAreAlsoAbsentFromTheEmptyState() {
        render(emptyState(availableChips = AccountDetailChip.entries.toSet() - GATED_CHIPS))
        composeRule.onNodeWithTag(AccountDetailTestTags.CHIP_ROW).performScrollTo()

        composeRule.onNodeWithTag(AccountDetailTestTags.chip(AccountDetailChip.StandingOrders))
            .assertDoesNotExist()
        composeRule.onNodeWithTag(AccountDetailTestTags.chip(AccountDetailChip.Transactions))
            .assertExists()
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
