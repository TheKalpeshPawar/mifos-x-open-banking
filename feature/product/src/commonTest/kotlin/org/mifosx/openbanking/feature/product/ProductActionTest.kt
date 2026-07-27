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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.product.ui.ProductAction
import org.mifosx.openbanking.feature.product.ui.ProductErrorKind
import org.mifosx.openbanking.feature.product.ui.ProductState
import org.mifosx.openbanking.feature.product.ui.ProductUiState
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Pins the actions each interactive surface dispatches. The product screen is read-only, so Retry is the
 * only control it has — the rest of the screen must dispatch nothing at all.
 */
@OptIn(ExperimentalTestApi::class)
class ProductActionTest {

    @Test
    fun retryDispatchesRetryLoad() = runComposeUiTest {
        val actions = mutableListOf<ProductAction>()
        setContent {
            ProductScreenContent(
                state = ProductState(uiState = ProductUiState.Error(ProductErrorKind.Network)),
                onAction = { actions += it },
            )
        }

        onNodeWithTag(ProductTestTags.RETRY_BUTTON).performClick()

        assertContentEquals(listOf(ProductAction.RetryLoad), actions)
    }

    @Test
    fun retryCanBeDispatchedRepeatedly() = runComposeUiTest {
        val actions = mutableListOf<ProductAction>()
        setContent {
            ProductScreenContent(
                state = ProductState(uiState = ProductUiState.Error(ProductErrorKind.TokenExpired)),
                onAction = { actions += it },
            )
        }

        onNodeWithTag(ProductTestTags.RETRY_BUTTON).performClick()
        onNodeWithTag(ProductTestTags.RETRY_BUTTON).performClick()

        assertContentEquals(listOf(ProductAction.RetryLoad, ProductAction.RetryLoad), actions)
    }

    @Test
    fun contentDispatchesNothing() = runComposeUiTest {
        val actions = mutableListOf<ProductAction>()
        setContent {
            ProductScreenContent(
                state = ProductState(uiState = ProductUiState.Content(ProductFixtures.uiModel())),
                onAction = { actions += it },
            )
        }

        onNodeWithTag(ProductTestTags.HEADER_CARD).performClick()
        onNodeWithTag(ProductTestTags.creditTier(0)).performClick()

        assertTrue(actions.isEmpty())
    }
}
