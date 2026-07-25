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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.product.ui.ProductErrorKind
import org.mifosx.openbanking.feature.product.ui.ProductState
import org.mifosx.openbanking.feature.product.ui.ProductUiState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import template.core.base.designsystem.KptTheme

private const val ROBOLECTRIC_SDK = 34
private const val FRAME_WIDTH = 412
private const val FRAME_HEIGHT = 892

/**
 * Golden-image coverage for [ProductScreenContent], captured with Roborazzi under Robolectric's native
 * graphics (no device). Goldens are committed under `src/androidUnitTest/screenshots/`;
 * `recordRoborazziDemoDebug` writes them and `verifyRoborazziDemoDebug` fails the build on pixel drift.
 *
 * Five rather than four: the content state is the one the design cares most about, and a business
 * account renders a different badge and a non-zero — untinted — monthly charge, so it is worth its own
 * golden.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class ProductScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentGolden() = capture(
        "content",
        ProductState(uiState = ProductUiState.Content(ProductFixtures.uiModel())),
    )

    @Test
    fun businessAccountGolden() = capture(
        "business",
        ProductState(
            uiState = ProductUiState.Content(
                ProductFixtures.uiModel().copy(
                    productType = "BCA",
                    productName = "HSBC Business Current Account",
                    monthlyMaximumChargeLabel = "£8.00",
                    isFeeFree = false,
                ),
            ),
        ),
    )

    @Test
    fun loadingGolden() = capture("loading", ProductState(uiState = ProductUiState.Loading))

    @Test
    fun emptyGolden() = capture("empty", ProductState(uiState = ProductUiState.Empty))

    @Test
    fun errorGolden() = capture(
        "error",
        ProductState(uiState = ProductUiState.Error(ProductErrorKind.ConsentScope)),
    )

    private fun capture(state: String, screenState: ProductState) {
        composeRule.setContent {
            Themed {
                ProductScreenContent(state = screenState, onAction = {})
            }
        }
        composeRule.onRoot().captureRoboImage("src/androidUnitTest/screenshots/product_$state.png")
    }

    @Composable
    private fun Themed(content: @Composable () -> Unit) {
        KptTheme {
            Surface(
                modifier = Modifier
                    .width(FRAME_WIDTH.dp)
                    .height(FRAME_HEIGHT.dp)
                    .background(MaterialTheme.colorScheme.background),
                content = content,
            )
        }
    }
}
