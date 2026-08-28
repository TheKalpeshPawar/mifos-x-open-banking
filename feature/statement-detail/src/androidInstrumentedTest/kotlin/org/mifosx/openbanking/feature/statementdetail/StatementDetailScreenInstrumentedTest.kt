/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.statementdetail.ui.StatementAmountColor
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailAction
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailUiModel
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailUiState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementLineUiModel
import org.mifosx.openbanking.feature.statementdetail.ui.StatementTxnRowUiModel
import kotlin.test.assertEquals

private const val ACCOUNT_ID = "40051512345678"
private const val STATEMENT_ID = "STMT-2026-05-40051512345678"
private const val FIRST_TXN_ID = "TXN-2026-05-001"

/**
 * androidInstrumentedTest does not see commonTest, so the state fixtures are inlined here.
 */
private fun statementModel(): StatementDetailUiModel = StatementDetailUiModel(
    reference = "MAY-2026-STMT",
    periodLabel = "1 May 2026 – 31 May 2026",
    type = "RegularPeriodic",
    createdDateLabel = "1 Jun 2026",
    balances = listOf(
        StatementLineUiModel("Opening Balance", "£2,610.40", StatementAmountColor.Credit),
        StatementLineUiModel("Closing Balance", "£2,847.63", StatementAmountColor.Credit),
    ),
    fees = listOf(StatementLineUiModel("Monthly maintenance fee", "£0.00", StatementAmountColor.Neutral)),
    interest = listOf(StatementLineUiModel("In-credit interest", "£0.21", StatementAmountColor.Credit)),
)

private fun rows(): List<StatementTxnRowUiModel> = listOf(
    StatementTxnRowUiModel(FIRST_TXN_ID, ACCOUNT_ID, "3 May 2026", "TESCO STORES 3225 LONDON", "-£82.50", false),
    StatementTxnRowUiModel(
        "TXN-2026-05-002",
        ACCOUNT_ID,
        "10 May 2026",
        "BACS CREDIT ACME CORP PAYROLL",
        "+£3,200.00",
        true,
    ),
)

private fun contentState(): StatementDetailState = StatementDetailState(
    accountId = ACCOUNT_ID,
    statementId = STATEMENT_ID,
    uiState = StatementDetailUiState.Content(statementModel(), rows()),
)

/**
 * On-device mirror of [StatementDetailScreenRobolectricTest], driving the same [StatementDetailTestTags]
 * so a divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class StatementDetailScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<StatementDetailAction>()
    private var navigated: Pair<String, String>? = null

    private fun render(state: StatementDetailState) {
        composeRule.setContent {
            StatementDetailScreenContent(
                state = state,
                onAction = { actions.add(it) },
                onRowClick = { transactionId, accountId -> navigated = transactionId to accountId },
            )
        }
    }

    private fun scrollTo(targetTag: String) {
        composeRule.onNodeWithTag(StatementDetailTestTags.CONTENT_LIST)
            .performScrollToNode(hasTestTag(targetTag))
    }

    @Test
    fun contentRendersHeaderAndFirstTransaction() {
        render(contentState())

        composeRule.onNodeWithTag(StatementDetailTestTags.CONTENT_LIST).assertExists()
        composeRule.onNodeWithTag(StatementDetailTestTags.HEADER_CARD, useUnmergedTree = true).assertExists()
        scrollTo(StatementDetailTestTags.txnRow(FIRST_TXN_ID))
        composeRule.onNodeWithTag(StatementDetailTestTags.txnRow(FIRST_TXN_ID)).assertExists()
    }

    @Test
    fun tappingATransactionRowRoutesBothIds() {
        render(contentState())

        scrollTo(StatementDetailTestTags.txnRow(FIRST_TXN_ID))
        composeRule.onNodeWithTag(StatementDetailTestTags.txnRow(FIRST_TXN_ID)).performClick()

        assertEquals(FIRST_TXN_ID to ACCOUNT_ID, navigated)
    }

    @Test
    fun tappingDownloadDispatchesDownloadPdf() {
        render(contentState())

        scrollTo(StatementDetailTestTags.DOWNLOAD_BUTTON)
        composeRule.onNodeWithTag(StatementDetailTestTags.DOWNLOAD_BUTTON).performClick()

        assertEquals(listOf<StatementDetailAction>(StatementDetailAction.DownloadPdf), actions)
    }
}
