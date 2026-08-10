/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

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
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesErrorKind
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import template.core.base.designsystem.KptTheme

private const val ROBOLECTRIC_SDK = 34
private const val FRAME_WIDTH = 412
private const val FRAME_HEIGHT = 892

/**
 * Golden-image coverage for [BeneficiariesScreenContent], captured with Roborazzi under Robolectric's
 * native graphics (no device). Goldens are written under `build/outputs/roborazzi/`;
 * `recordRoborazziDebug` writes them and `verifyRoborazziDebug` fails the build on any pixel drift.
 *
 * Six states rather than the usual four: this screen has two mutually exclusive error variants
 * (Retry versus View Consents) and an in-content no-results block that is distinct from the empty
 * state, and each of those is a rendering a reviewer should be able to see change.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class BeneficiariesScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentGolden() = capture("content", BeneficiariesFixtures.contentState())

    @Test
    fun loadingGolden() = capture("loading", BeneficiariesFixtures.loadingState())

    @Test
    fun emptyGolden() = capture("empty", BeneficiariesFixtures.emptyState())

    @Test
    fun errorGolden() = capture("error", BeneficiariesFixtures.errorState(BeneficiariesErrorKind.TokenExpired))

    @Test
    fun consentRevokedGolden() =
        capture("consent_revoked", BeneficiariesFixtures.errorState(BeneficiariesErrorKind.ConsentRevoked))

    @Test
    fun searchNoResultsGolden() = capture("search_no_results", BeneficiariesFixtures.searchWithoutMatchesState())

    private fun capture(state: String, screenState: BeneficiariesState) {
        composeRule.setContent {
            Themed {
                BeneficiariesScreenContent(
                    state = screenState,
                    onAction = {},
                    onNavigateToConsents = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/beneficiaries_$state.png")
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
