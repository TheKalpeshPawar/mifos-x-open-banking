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

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.product.ui.ProductErrorKind
import org.mifosx.openbanking.feature.product.ui.ProductState
import org.mifosx.openbanking.feature.product.ui.ProductUiState
import kotlin.test.Test

/** The content state driven by the shared fixture product. */
@Composable
private fun FixtureContentState() {
    ProductScreenContent(
        state = ProductState(uiState = ProductUiState.Content(ProductFixtures.uiModel())),
        onAction = {},
    )
}

/**
 * Renders each product state through the desktop Compose runner. Excluded from the JVM unit-test tasks by
 * the `*ScreenUiTest` filter in this module's build script — there is no Robolectric runner there.
 */
@OptIn(ExperimentalTestApi::class)
class ProductScreenUiTest {

    @Test
    fun contentRendersEverySection() = runComposeUiTest {
        setContent { FixtureContentState() }

        onNodeWithTag(ProductTestTags.CONTENT).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.HEADER_CARD).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.FEES_SECTION).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.CREDIT_INTEREST_SECTION).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.OVERDRAFT_SECTION).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.FEATURES_SECTION).assertIsDisplayed()
    }

    @Test
    fun contentRendersTheProductIdentity() = runComposeUiTest {
        setContent { FixtureContentState() }

        onNodeWithTag(ProductTestTags.TYPE_BADGE).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.PRODUCT_NAME).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.PRODUCT_ID).assertIsDisplayed()
    }

    @Test
    fun contentRendersOneRowPerTier() = runComposeUiTest {
        setContent { FixtureContentState() }

        onNodeWithTag(ProductTestTags.creditTier(0)).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.creditTier(1)).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.overdraftTier(0)).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.overdraftTier(1)).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.feature(0)).assertIsDisplayed()
    }

    @Test
    fun loadingRendersTheIndicator() = runComposeUiTest {
        setContent {
            ProductScreenContent(state = ProductState(uiState = ProductUiState.Loading), onAction = {})
        }

        onNodeWithTag(ProductTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun emptyRendersWithoutARetryButton() = runComposeUiTest {
        setContent {
            ProductScreenContent(state = ProductState(uiState = ProductUiState.Empty), onAction = {})
        }

        onNodeWithTag(ProductTestTags.EMPTY).assertIsDisplayed()
        // The empty state is informational, so it deliberately offers no Retry.
        onNodeWithTag(ProductTestTags.RETRY_BUTTON).assertDoesNotExist()
    }

    @Test
    fun errorRendersWithARetryButton() = runComposeUiTest {
        setContent {
            ProductScreenContent(
                state = ProductState(
                    uiState = ProductUiState.Error(ProductErrorKind.ConsentScope),
                ),
                onAction = {},
            )
        }

        onNodeWithTag(ProductTestTags.ERROR).assertIsDisplayed()
        onNodeWithTag(ProductTestTags.RETRY_BUTTON).assertIsDisplayed()
    }
}
