/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.login.ui.LoginAction
import org.mifosx.openbanking.feature.login.ui.LoginUiState
import kotlin.test.assertEquals

/**
 * The login states on a real device / emulator. The same surfaces are covered device-free by
 * `LoginScreenRobolectricTest`; this class exercises them against the on-device Compose runtime.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateRenders() {
        composeRule.setContent { LoadingState() }

        composeRule.onNodeWithTag(LoginTestTags.LOADING_PROGRESS).assertExists()
    }

    @Test
    fun authorisingStateRenders() {
        composeRule.setContent { AuthorisingState() }

        composeRule.onNodeWithTag(LoginTestTags.AUTHORISING).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.AUTHORISING_SPINNER).assertExists()
    }

    @Test
    fun contentStateContinueDispatchesStartOAuth() {
        val actions = mutableListOf<LoginAction>()
        composeRule.setContent {
            ContentState(
                state = LoginUiState.Content(permissions = emptyList(), expiryLabel = "Expires 1 Jan 2027"),
                onAction = { actions += it },
            )
        }

        composeRule.onNodeWithTag(LoginTestTags.EXPLAINER_CARD).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.CONTINUE_HSBC).performClick()

        assertEquals(listOf<LoginAction>(LoginAction.StartOAuth), actions)
    }

    @Test
    fun errorStateRetryDispatchesRetry() {
        val actions = mutableListOf<LoginAction>()
        composeRule.setContent {
            ErrorState(state = LoginUiState.Error("Could not connect"), onAction = { actions += it })
        }

        composeRule.onNodeWithTag(LoginTestTags.ERROR).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.ERROR_RETRY).performClick()

        assertEquals(listOf<LoginAction>(LoginAction.Retry), actions)
    }

    @Test
    fun emptyStateGoBackDispatchesCancel() {
        val actions = mutableListOf<LoginAction>()
        composeRule.setContent { EmptyState(onAction = { actions += it }) }

        composeRule.onNodeWithTag(LoginTestTags.EMPTY).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.EMPTY_GO_BACK).performClick()

        assertEquals(listOf<LoginAction>(LoginAction.Cancel), actions)
    }
}
