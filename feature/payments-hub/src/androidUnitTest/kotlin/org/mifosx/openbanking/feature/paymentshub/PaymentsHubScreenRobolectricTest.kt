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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.paymentshub.ui.PaymentsHubState
import org.mifosx.openbanking.feature.paymentshub.ui.PaymentsHubUiState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val ROBOLECTRIC_SDK = 34

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class PaymentsHubScreenRobolectricTest {

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
    fun contentRendersQuickActions() {
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
    fun errorRendersScreen() {
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
