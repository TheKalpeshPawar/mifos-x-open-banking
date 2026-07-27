/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.product

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.product.ui.ProductAction
import org.mifosx.openbanking.feature.product.ui.ProductErrorKind
import org.mifosx.openbanking.feature.product.ui.ProductState
import org.mifosx.openbanking.feature.product.ui.ProductUiState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertContentEquals

private const val ROBOLECTRIC_SDK = 34

/**
 * The product states against the real Android Compose runtime under Robolectric — JVM, no device. The
 * sibling `ProductScreenInstrumentedTest` runs the same assertions on a real device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class ProductScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showContent() = composeRule.setContent {
        ProductScreenContent(
            state = ProductState(uiState = ProductUiState.Content(ProductFixtures.uiModel())),
            onAction = {},
        )
    }

    @Test
    fun contentRendersAllSections() {
        showContent()

        composeRule.onNodeWithTag(ProductTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.HEADER_CARD).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.FEES_SECTION).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.CREDIT_INTEREST_SECTION).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.OVERDRAFT_SECTION).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.FEATURES_SECTION).assertExists()
    }

    @Test
    fun contentRendersTheTypeBadgeAndName() {
        showContent()

        composeRule.onNodeWithTag(ProductTestTags.TYPE_BADGE).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.PRODUCT_NAME).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.PRODUCT_ID).assertExists()
    }

    @Test
    fun contentRendersEveryTierRow() {
        showContent()

        composeRule.onNodeWithTag(ProductTestTags.creditTier(0)).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.creditTier(1)).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.overdraftTier(0)).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.overdraftTier(1)).assertExists()
    }

    @Test
    fun loadingStateRenders() {
        composeRule.setContent {
            ProductScreenContent(state = ProductState(uiState = ProductUiState.Loading), onAction = {})
        }

        composeRule.onNodeWithTag(ProductTestTags.LOADING).assertExists()
    }

    @Test
    fun emptyStateRendersWithoutRetry() {
        composeRule.setContent {
            ProductScreenContent(state = ProductState(uiState = ProductUiState.Empty), onAction = {})
        }

        composeRule.onNodeWithTag(ProductTestTags.EMPTY).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.RETRY_BUTTON).assertDoesNotExist()
    }

    @Test
    fun errorStateRendersWithRetry() {
        composeRule.setContent {
            ProductScreenContent(
                state = ProductState(uiState = ProductUiState.Error(ProductErrorKind.ConsentScope)),
                onAction = {},
            )
        }

        composeRule.onNodeWithTag(ProductTestTags.ERROR).assertExists()
        composeRule.onNodeWithTag(ProductTestTags.RETRY_BUTTON).assertExists()
    }

    @Test
    fun errorRetryDispatchesRetryLoad() {
        val actions = mutableListOf<ProductAction>()
        composeRule.setContent {
            ProductScreenContent(
                state = ProductState(uiState = ProductUiState.Error(ProductErrorKind.Network)),
                onAction = { actions += it },
            )
        }

        composeRule.onNodeWithTag(ProductTestTags.RETRY_BUTTON).performClick()

        assertContentEquals(listOf(ProductAction.RetryLoad), actions)
    }
}
