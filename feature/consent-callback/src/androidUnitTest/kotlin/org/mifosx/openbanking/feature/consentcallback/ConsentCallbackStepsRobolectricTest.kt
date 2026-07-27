/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentcallback

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.consentcallback.steps.AccessDeniedStep
import org.mifosx.openbanking.feature.consentcallback.steps.AwaitingStep
import org.mifosx.openbanking.feature.consentcallback.steps.CallbackErrorStep
import org.mifosx.openbanking.feature.consentcallback.steps.LoadingStep
import org.mifosx.openbanking.feature.consentcallback.steps.SecurityErrorStep
import org.mifosx.openbanking.feature.consentcallback.steps.SuccessStep
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackAction
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * The consent-callback steps against the real **Android** Compose runtime.
 *
 * The sibling `ConsentCallbackStepsUiTest` in `commonTest` covers the same surfaces on the desktop
 * renderer. This one exists because that is not the runtime the app ships on Android, and it needs a
 * separate class: Robolectric is driven by `@RunWith`, which is a JVM annotation `commonTest` cannot
 * carry. Robolectric keeps it on the JVM, so it still runs in CI and on a Linux box with no device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class ConsentCallbackStepsRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStepRenders() {
        composeRule.setContent { LoadingStep() }

        composeRule.onNodeWithTag(ConsentCallbackTestTags.LOADING_STEP).assertExists()
    }

    @Test
    fun successStepRenders() {
        composeRule.setContent { SuccessStep() }

        composeRule.onNodeWithTag(ConsentCallbackTestTags.SUCCESS_STEP).assertExists()
    }

    @Test
    fun awaitingStepCheckAgainDispatchesPollConsentStatus() {
        val actions = mutableListOf<ConsentCallbackAction>()
        composeRule.setContent { AwaitingStep(onAction = { actions += it }) }

        composeRule.onNodeWithTag(ConsentCallbackTestTags.CHECK_AGAIN_BUTTON).performClick()

        assertEquals(listOf<ConsentCallbackAction>(ConsentCallbackAction.PollConsentStatus), actions)
    }

    @Test
    fun errorStepRetryDispatchesNavigateRetry() {
        val actions = mutableListOf<ConsentCallbackAction>()
        composeRule.setContent { CallbackErrorStep(onAction = { actions += it }) }

        composeRule.onNodeWithTag(ConsentCallbackTestTags.ERROR_RETRY_BUTTON).performClick()

        assertEquals(listOf<ConsentCallbackAction>(ConsentCallbackAction.NavigateRetry), actions)
    }

    @Test
    fun accessDeniedStepStartOverDispatchesNavigateRetry() {
        val actions = mutableListOf<ConsentCallbackAction>()
        composeRule.setContent { AccessDeniedStep(onAction = { actions += it }) }

        composeRule.onNodeWithTag(ConsentCallbackTestTags.START_OVER_BUTTON).performClick()

        assertEquals(listOf<ConsentCallbackAction>(ConsentCallbackAction.NavigateRetry), actions)
    }

    @Test
    fun securityErrorStepStartAgainDispatchesNavigateLogin() {
        val actions = mutableListOf<ConsentCallbackAction>()
        composeRule.setContent { SecurityErrorStep(onAction = { actions += it }) }

        composeRule.onNodeWithTag(ConsentCallbackTestTags.START_AGAIN_BUTTON).performClick()

        assertEquals(listOf<ConsentCallbackAction>(ConsentCallbackAction.NavigateLogin), actions)
    }
}

private const val ROBOLECTRIC_SDK = 34
