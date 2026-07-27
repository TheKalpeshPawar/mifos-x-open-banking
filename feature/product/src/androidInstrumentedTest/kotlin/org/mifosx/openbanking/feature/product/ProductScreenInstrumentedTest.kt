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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.product.ui.CreditTierUiModel
import org.mifosx.openbanking.feature.product.ui.OverdraftTierUiModel
import org.mifosx.openbanking.feature.product.ui.ProductAction
import org.mifosx.openbanking.feature.product.ui.ProductErrorKind
import org.mifosx.openbanking.feature.product.ui.ProductState
import org.mifosx.openbanking.feature.product.ui.ProductUiModel
import org.mifosx.openbanking.feature.product.ui.ProductUiState
import kotlin.test.assertContentEquals

/**
 * The product states on a real device or emulator. The same surfaces are covered device-free by
 * `ProductScreenRobolectricTest`.
 *
 * The fixture is inlined rather than shared: `androidInstrumentedTest` is a separate compilation and
 * cannot see `commonTest`, so `ProductFixtures` is invisible here.
 */
@RunWith(AndroidJUnit4::class)
class ProductScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showContent() = composeRule.setContent {
        ProductScreenContent(
            state = ProductState(uiState = ProductUiState.Content(sampleProduct())),
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

private fun sampleProduct(): ProductUiModel = ProductUiModel(
    productType = "PCA",
    productName = "HSBC Advance Account",
    productId = "HSBC-ADVANCE-PCA-001",
    monthlyMaximumChargeLabel = "£0.00",
    isFeeFree = true,
    creditInterestTiers = listOf(
        CreditTierUiModel(bandLimit = "£1,000", aer = "0.00", applicationFrequency = "Monthly"),
        CreditTierUiModel(bandLimit = "£10,000", aer = "0.15", applicationFrequency = "Monthly"),
    ),
    overdraftTiers = listOf(
        OverdraftTierUiModel(overdraftType = "Arranged", ear = "39.9"),
        OverdraftTierUiModel(overdraftType = "Unarranged", ear = "49.9"),
    ),
    features = listOf(
        "No monthly maintenance fee",
        "Mobile and online banking included",
    ),
)
