/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentshub

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.paymentshub.ui.PaymentsHubState
import org.mifosx.openbanking.feature.paymentshub.ui.PaymentsHubUiState

@RunWith(AndroidJUnit4::class)
class PaymentsHubScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingRendersTheSkeleton() {
        composeRule.setContent {
            PaymentsHubContent(
                state = PaymentsHubState(uiState = PaymentsHubUiState.Loading),
                onAction = {},
                onNavigateToSendMoney = {},
                onNavigateToPaymentStatus = {},
            )
        }
        composeRule.onNodeWithTag(PaymentsHubTestTags.SKELETON).assertIsDisplayed()
    }

    @Test
    fun contentRendersQuickActionsGrid() {
        composeRule.setContent {
            PaymentsHubContent(
                state = PaymentsHubState(
                    uiState = PaymentsHubUiState.Content(activityItems = emptyList()),
                ),
                onAction = {},
                onNavigateToSendMoney = {},
                onNavigateToPaymentStatus = {},
            )
        }
        composeRule.onNodeWithTag(PaymentsHubTestTags.QUICK_ACTION_SEND_MONEY).assertIsDisplayed()
    }

    @Test
    fun errorRendersRetryButton() {
        composeRule.setContent {
            PaymentsHubContent(
                state = PaymentsHubState(
                    uiState = PaymentsHubUiState.Error("Connection failed"),
                ),
                onAction = {},
                onNavigateToSendMoney = {},
                onNavigateToPaymentStatus = {},
            )
        }
        composeRule.onNodeWithTag(PaymentsHubTestTags.ERROR_SCREEN).assertIsDisplayed()
    }
}
