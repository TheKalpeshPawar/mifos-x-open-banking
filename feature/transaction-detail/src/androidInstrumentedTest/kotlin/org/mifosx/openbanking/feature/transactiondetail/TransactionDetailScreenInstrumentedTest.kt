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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailAction
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailState
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailUiModel
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailUiState
import kotlin.test.assertEquals

private const val ACCOUNT_ID = "40051512345678"
private const val DEBIT_ID = "TX-20260626-0001"
private const val REFERENCE = "TESCO STORES 3476 LONDON"

/**
 * androidInstrumentedTest does not see commonTest, so the state fixtures are inlined here.
 */
private fun debitUiModel(): TransactionDetailUiModel = TransactionDetailUiModel(
    amountLabel = "-£42.17",
    isCredit = false,
    currencyLabel = "GBP",
    merchantLabel = "Tesco Stores",
    status = "Booked",
    isBooked = true,
    bookingDateLabel = "26 Jun 2026, 11:22",
    valueDateLabel = "26 Jun 2026, 11:22",
    category = TransactionCategory.GROCERIES,
    merchantCategoryCode = "5411",
    balanceAfterLabel = "£447.63",
    referenceLabel = REFERENCE,
    bankCodeLabel = "DR · HSBC",
)

private fun creditUiModel(): TransactionDetailUiModel = TransactionDetailUiModel(
    amountLabel = "+£2,400.00",
    isCredit = true,
    currencyLabel = "GBP",
    merchantLabel = "SALARY JUN ACME LTD",
    status = "Booked",
    isBooked = true,
    bookingDateLabel = "25 Jun 2026, 08:00",
    valueDateLabel = "25 Jun 2026, 00:00",
    category = TransactionCategory.OTHER,
    merchantCategoryCode = null,
    balanceAfterLabel = "£2,847.63",
    referenceLabel = "SALARY JUN ACME LTD",
    bankCodeLabel = "CR · HSBC",
)

private fun contentState(model: TransactionDetailUiModel = debitUiModel()): TransactionDetailState =
    TransactionDetailState(
        transactionId = DEBIT_ID,
        accountId = ACCOUNT_ID,
        uiState = TransactionDetailUiState.Content(model),
    )

private fun mccNullContentState(): TransactionDetailState = contentState(creditUiModel())

private fun emptyState(): TransactionDetailState = TransactionDetailState(
    transactionId = DEBIT_ID,
    accountId = ACCOUNT_ID,
    uiState = TransactionDetailUiState.Empty,
)

/**
 * On-device mirror of [TransactionDetailScreenRobolectricTest], driving the same
 * [TransactionDetailTestTags] so a divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class TransactionDetailScreenInstrumentedTest {

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
        render(contentState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.CONTENT_ROOT).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.AMOUNT).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.MERCHANT).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.STATUS_CHIP).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.DETAIL_CARD).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.REFERENCE_ROW).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.MCC_ROW).assertExists()
    }

    @Test
    fun aTransactionWithoutAnMccHidesTheMccRow() {
        render(mccNullContentState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.MCC_ROW).assertDoesNotExist()
    }

    @Test
    fun tappingTheReferenceRowDispatchesCopyReference() {
        render(contentState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.REFERENCE_ROW).performScrollTo().performClick()

        assertEquals(
            listOf<TransactionDetailAction>(TransactionDetailAction.CopyReference(REFERENCE)),
            actions,
        )
    }

    @Test
    fun emptyRendersItsTitleAndBackButton() {
        render(emptyState())

        composeRule.onNodeWithTag(TransactionDetailTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.EMPTY_TITLE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(TransactionDetailTestTags.EMPTY_BACK_BUTTON).performClick()

        assertEquals(1, backCount)
    }
}
