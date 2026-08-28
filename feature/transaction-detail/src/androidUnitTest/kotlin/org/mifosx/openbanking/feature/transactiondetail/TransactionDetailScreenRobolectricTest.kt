/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailAction
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [TransactionDetailScreenContent] across its states under Robolectric (JVM, no device) and
 * drives it through the shared [TransactionDetailTestTags]. A verbatim on-device mirror lives in
 * [TransactionDetailScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class TransactionDetailScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<TransactionDetailAction>()
    private var backCount = 0

    private fun render(state: TransactionDetailState) {
        composeRule.setContent {
            TransactionDetailScreenContent(
                state = state,
                onAction = { actions.add(it) },
                onBack = { backCount++ },
            )
        }
    }

    @Test
    fun contentRendersTheAmountMerchantStatusDetailRowsAndReference() {
        render(TransactionDetailFixtures.contentState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.CONTENT_ROOT).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.AMOUNT).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.MERCHANT).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.STATUS_CHIP).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.DETAIL_CARD).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.BOOKING_DATE_ROW).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.VALUE_DATE_ROW).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.CATEGORY_ROW).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.BALANCE_AFTER_ROW).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.REFERENCE_ROW).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.BANK_CODE_ROW).assertExists()
    }

    @Test
    fun aTransactionWithAnMccShowsTheMccRow() {
        render(TransactionDetailFixtures.contentState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.MCC_ROW).assertExists()
    }

    @Test
    fun aTransactionWithoutAnMccHidesTheMccRow() {
        render(TransactionDetailFixtures.mccNullContentState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.MCC_ROW).assertDoesNotExist()
    }

    @Test
    fun tappingTheReferenceRowDispatchesCopyReference() {
        render(TransactionDetailFixtures.contentState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.REFERENCE_ROW).performScrollTo().performClick()

        assertEquals(
            listOf<TransactionDetailAction>(
                TransactionDetailAction.CopyReference("TESCO STORES 3476 LONDON"),
            ),
            actions,
        )
    }

    @Test
    fun emptyRendersItsTitleAndBackButton() {
        render(TransactionDetailFixtures.emptyState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.EMPTY_TITLE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TransactionDetailTestTags.EMPTY_BACK_BUTTON).assertExists()
    }

    @Test
    fun tappingTheEmptyBackButtonInvokesOnBack() {
        render(TransactionDetailFixtures.emptyState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.EMPTY_BACK_BUTTON).performClick()

        assertEquals(1, backCount)
    }
}
