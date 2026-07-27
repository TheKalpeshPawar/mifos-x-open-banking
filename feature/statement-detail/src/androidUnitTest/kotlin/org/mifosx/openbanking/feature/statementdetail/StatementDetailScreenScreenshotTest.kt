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
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import template.core.base.designsystem.KptTheme

private const val ROBOLECTRIC_SDK = 34
private const val FRAME_WIDTH = 412
private const val FRAME_HEIGHT = 892

/**
 * Golden-image coverage for [StatementDetailScreenContent] across its four rendered states, captured
 * with Roborazzi under Robolectric's native graphics (no device). Goldens are committed under
 * `src/androidUnitTest/screenshots/`; `recordRoborazziDemoDebug` writes them and
 * `verifyRoborazziDemoDebug` fails the build on any pixel drift.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class StatementDetailScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentGolden() = capture("content", StatementDetailFixtures.contentState())

    @Test
    fun loadingGolden() = capture("loading", StatementDetailFixtures.loadingState())

    @Test
    fun emptyGolden() = capture("empty", StatementDetailFixtures.emptyState())

    @Test
    fun errorGolden() = capture("error", StatementDetailFixtures.errorState())

    private fun capture(state: String, screenState: StatementDetailState) {
        composeRule.setContent {
            Themed {
                StatementDetailScreenContent(
                    state = screenState,
                    onAction = {},
                    onRowClick = { _, _ -> },
                )
            }
        }
        composeRule.onRoot().captureRoboImage("src/androidUnitTest/screenshots/statement_detail_$state.png")
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
