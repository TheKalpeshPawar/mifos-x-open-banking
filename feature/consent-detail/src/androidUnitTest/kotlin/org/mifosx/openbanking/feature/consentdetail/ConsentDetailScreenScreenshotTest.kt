/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail

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
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import template.core.base.designsystem.KptTheme

private const val ROBOLECTRIC_SDK = 34
private const val FRAME_WIDTH = 412
private const val FRAME_HEIGHT = 892
private const val WARNING_DAYS = 3

/**
 * Golden-image coverage for [ConsentDetailScreenContent], captured with Roborazzi under
 * Robolectric's native graphics (no device).
 *
 * Six states rather than four: the two extra ones are the revoke gate and the in-flight revoking
 * screen, which are the whole reason this feature is more than a read-only detail page and are
 * exactly where a visual regression would be most costly.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class ConsentDetailScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentGolden() = capture("content", ConsentDetailFixtures.contentState())

    @Test
    fun expiryWarningGolden() =
        capture("expiry_warning", ConsentDetailFixtures.contentState(expiryWarningDays = WARNING_DAYS))

    @Test
    fun revokeConfirmGolden() = capture("revoke_confirm", ConsentDetailFixtures.revokeConfirmState())

    @Test
    fun revokingGolden() = capture("revoking", ConsentDetailFixtures.revokingState())

    @Test
    fun loadingGolden() = capture("loading", ConsentDetailFixtures.loadingState())

    @Test
    fun emptyGolden() = capture("empty", ConsentDetailFixtures.emptyState())

    @Test
    fun errorGolden() = capture("error", ConsentDetailFixtures.errorState())

    private fun capture(state: String, screenState: ConsentDetailState) {
        composeRule.setContent {
            Themed {
                ConsentDetailScreenContent(
                    state = screenState,
                    onAction = {},
                    onReconfirm = {},
                    onGoBack = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("src/androidUnitTest/screenshots/consent_detail_$state.png")
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
