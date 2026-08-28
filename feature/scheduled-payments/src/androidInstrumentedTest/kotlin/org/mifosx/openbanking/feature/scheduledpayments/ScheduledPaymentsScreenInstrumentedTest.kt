/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.scheduledpayments

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentType
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentUiModel
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsState
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsUiState

private const val ACCOUNT_ID = "40051512345678"
private const val EXECUTION_ID = "SP-001"
private const val ARRIVAL_ID = "SP-003"

/**
 * androidInstrumentedTest does not see commonTest, so the state fixtures are inlined here.
 */
private fun executionUiModel(): ScheduledPaymentUiModel = ScheduledPaymentUiModel(
    scheduledPaymentId = EXECUTION_ID,
    payeeName = "HMRC Self Assessment",
    amountLabel = "£842.00",
    scheduledDateLabel = "Fri 31 Jul 2026",
    scheduledType = ScheduledPaymentType.Execution,
    creditorIdentification = "08-32-00 12001039",
    reference = "HMRC-SA-2526",
)

private fun arrivalUiModel(): ScheduledPaymentUiModel = ScheduledPaymentUiModel(
    scheduledPaymentId = ARRIVAL_ID,
    payeeName = "Direct Line Insurance",
    amountLabel = "£412.50",
    scheduledDateLabel = "Sat 15 Aug 2026",
    scheduledType = ScheduledPaymentType.Arrival,
    creditorIdentification = "20-00-00 73428901",
    reference = "DL-HOME-INS-26",
)

private fun contentState(): ScheduledPaymentsState = ScheduledPaymentsState(
    accountId = ACCOUNT_ID,
    uiState = ScheduledPaymentsUiState.Content(listOf(executionUiModel(), arrivalUiModel())),
)

/**
 * On-device mirror of [ScheduledPaymentsScreenRobolectricTest], driving the same
 * [ScheduledPaymentsTestTags] so a divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class ScheduledPaymentsScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(state: ScheduledPaymentsState) {
        composeRule.setContent {
            ScheduledPaymentsScreenContent(state = state, onAction = {})
        }
    }

    @Test
    fun contentRendersTheCardsAndChips() {
        render(contentState())

        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.CONTENT_LIST).assertExists()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.card(EXECUTION_ID)).assertExists()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.card(ARRIVAL_ID)).assertExists()
    }
}
