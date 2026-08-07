/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyErrorKind
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import template.core.base.designsystem.KptTheme

private const val ROBOLECTRIC_SDK = 34

/**
 * A tall device, because the form scrolls well past a phone screen.
 *
 * This is a Robolectric qualifier rather than a Surface size: Roborazzi captures the composition
 * root, which Robolectric sizes from the device, so constraining the Surface changes nothing about
 * what lands in the image. At the default height the visible region stopped inside the payer list,
 * which made the domestic and international goldens differ only by which toggle segment was
 * highlighted — every field the two rails actually disagree about was below the fold.
 */
private const val TALL_DEVICE = "w412dp-h1800dp"

/**
 * Golden-image coverage for [SendMoneyScreenContent], captured with Roborazzi under Robolectric's
 * native graphics. Goldens live in `src/androidUnitTest/screenshots/`; `recordRoborazziDebug` writes
 * them and `verifyRoborazziDebug` fails the build on pixel drift.
 *
 * The two rails get separate goldens because they are not a styling variation — each asks for fields
 * the other refuses, so the difference between them is the thing most worth being able to see.
 *
 * These also guard the templated strings: a literal `%%` renders as `%%` in Compose Multiplatform
 * resources, and no tag or count assertion catches that.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK], qualifiers = TALL_DEVICE)
class SendMoneyScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun domesticFormGolden() = capture("form_domestic", SendMoneyFixtures.formState())

    @Test
    fun internationalFormGolden() =
        capture("form_international", SendMoneyFixtures.formState(rail = PaymentRail.International))

    /** The payer question open: the bank-choice row and the payee notice both showing. */
    @Test
    fun noPayerChosenGolden() =
        capture("form_no_payer", SendMoneyFixtures.formState(debtorAccountId = null))

    @Test
    fun nonSterlingPayerGolden() =
        capture("form_non_sterling_payer", SendMoneyFixtures.formState(debtorCurrency = "USD"))

    @Test
    fun manualIbanEntryGolden() = capture(
        "form_manual_iban",
        SendMoneyFixtures.formState(rail = PaymentRail.International, manualEntryVisible = true),
    )

    @Test
    fun reviewGolden() = capture("review", SendMoneyFixtures.reviewState())

    @Test
    fun loadingGolden() = capture("loading", SendMoneyFixtures.loadingState())

    @Test
    fun submittingGolden() = capture("submitting", SendMoneyFixtures.submittingState())

    /** The payer refusal, whose whole point is that it offers a different account and not Retry. */
    @Test
    fun payerNotSupportedGolden() =
        capture("error_payer", SendMoneyFixtures.errorState(SendMoneyErrorKind.PayerNotSupported))

    private fun capture(state: String, screenState: SendMoneyState) {
        composeRule.setContent {
            KptTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    SendMoneyScreenContent(state = screenState, onAction = {}, onNavigateToConsents = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/androidUnitTest/screenshots/send_money_$state.png")
    }
}
