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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.consentcallback.steps.AccessDeniedStep
import org.mifosx.openbanking.feature.consentcallback.steps.AwaitingStep
import org.mifosx.openbanking.feature.consentcallback.steps.CallbackErrorStep
import org.mifosx.openbanking.feature.consentcallback.steps.LoadingStep
import org.mifosx.openbanking.feature.consentcallback.steps.SecurityErrorStep
import org.mifosx.openbanking.feature.consentcallback.steps.SuccessStep
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackAction
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * Renders each consent-callback state and exercises every declared `on_click` action.
 *
 * Lives in `commonTest` so one source runs twice: headless on the JVM for desktop, and against the
 * real Android Compose runtime under Robolectric. Neither needs a device.
 */
@OptIn(ExperimentalTestApi::class)
class ConsentCallbackStepsUiTest {

    @Test
    fun `loading step renders`() = runComposeUiTest {
        setContent { LoadingStep() }

        onNodeWithTag(ConsentCallbackTestTags.LOADING_STEP).assertIsDisplayed()
    }

    @Test
    fun `success step renders`() = runComposeUiTest {
        setContent { SuccessStep() }

        onNodeWithTag(ConsentCallbackTestTags.SUCCESS_STEP).assertIsDisplayed()
    }

    @Test
    fun `awaiting step renders`() = runComposeUiTest {
        setContent { AwaitingStep(onAction = {}) }

        onNodeWithTag(ConsentCallbackTestTags.AWAITING_STEP).assertIsDisplayed()
    }

    @Test
    fun `awaiting step check again button dispatches PollConsentStatus`() = runComposeUiTest {
        val actions = mutableListOf<ConsentCallbackAction>()
        setContent { AwaitingStep(onAction = { actions += it }) }

        onNodeWithTag(ConsentCallbackTestTags.CHECK_AGAIN_BUTTON).performClick()

        assertContentEquals(listOf(ConsentCallbackAction.PollConsentStatus), actions)
    }

    @Test
    fun `error step renders`() = runComposeUiTest {
        setContent { CallbackErrorStep(onAction = {}) }

        onNodeWithTag(ConsentCallbackTestTags.ERROR_STEP).assertIsDisplayed()
    }

    @Test
    fun `error step retry button dispatches NavigateRetry`() = runComposeUiTest {
        val actions = mutableListOf<ConsentCallbackAction>()
        setContent { CallbackErrorStep(onAction = { actions += it }) }

        onNodeWithTag(ConsentCallbackTestTags.ERROR_RETRY_BUTTON).performClick()

        assertContentEquals(listOf(ConsentCallbackAction.NavigateRetry), actions)
    }

    @Test
    fun `access denied step renders`() = runComposeUiTest {
        setContent { AccessDeniedStep(onAction = {}) }

        onNodeWithTag(ConsentCallbackTestTags.ACCESS_DENIED_STEP).assertIsDisplayed()
    }

    @Test
    fun `access denied step start over button dispatches NavigateRetry`() = runComposeUiTest {
        val actions = mutableListOf<ConsentCallbackAction>()
        setContent { AccessDeniedStep(onAction = { actions += it }) }

        onNodeWithTag(ConsentCallbackTestTags.START_OVER_BUTTON).performClick()

        assertContentEquals(listOf(ConsentCallbackAction.NavigateRetry), actions)
    }

    @Test
    fun `security error step renders`() = runComposeUiTest {
        setContent { SecurityErrorStep(onAction = {}) }

        onNodeWithTag(ConsentCallbackTestTags.SECURITY_ERROR_STEP).assertIsDisplayed()
    }

    @Test
    fun `security error step start again button dispatches NavigateLogin`() = runComposeUiTest {
        val actions = mutableListOf<ConsentCallbackAction>()
        setContent { SecurityErrorStep(onAction = { actions += it }) }

        onNodeWithTag(ConsentCallbackTestTags.START_AGAIN_BUTTON).performClick()

        assertContentEquals(listOf(ConsentCallbackAction.NavigateLogin), actions)
    }
}
