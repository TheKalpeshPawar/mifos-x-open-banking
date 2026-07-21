/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.profile.ui.ProfileAction
import org.mifosx.openbanking.feature.profile.ui.ProfileErrorKind
import org.mifosx.openbanking.feature.profile.ui.ProfileState
import org.mifosx.openbanking.feature.profile.ui.ProfileUiState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Headless smoke pass over [ProfileScreenContent], covering the state fork itself rather than the
 * detail of each surface — that is what the Robolectric and instrumented suites are for.
 *
 * Lives in commonTest so it compiles for every target, and is excluded from the Android unit-test
 * tasks by the module's build script: there is no Robolectric runner there to supply an Android
 * runtime, so every case would NPE.
 */
@OptIn(ExperimentalTestApi::class)
class ProfileScreenUiTest {

    @Test
    fun loadingRendersTheSpinner() = runComposeUiTest {
        setContent {
            ProfileScreenContent(
                state = ProfileState(
                    accountId = ProfileFixtures.ACCOUNT_ID,
                    uiState = ProfileUiState.Loading,
                ),
                onAction = {},
            )
        }

        onNodeWithTag(ProfileTestTags.LOADING).assertIsDisplayed()
        onNodeWithTag(ProfileTestTags.CONTENT).assertDoesNotExist()
    }

    @Test
    fun contentRendersTheIdentityAndConnectionCards() = runComposeUiTest {
        setContent { ProfileScreenContent(state = ProfileFixtures.contentState(), onAction = {}) }

        onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertExists()
        onNodeWithTag(ProfileTestTags.CONNECTION_CARD, useUnmergedTree = true).assertExists()
        onNodeWithTag(ProfileTestTags.EXPIRY_BANNER, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun expiringContentRaisesTheBanner() = runComposeUiTest {
        setContent { ProfileScreenContent(state = ProfileFixtures.expiringState(), onAction = {}) }

        onNodeWithTag(ProfileTestTags.EXPIRY_BANNER, useUnmergedTree = true).assertExists()
        onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertExists()
    }

    @Test
    fun theSignOutDialogLeavesTheContentTreeStanding() = runComposeUiTest {
        setContent {
            ProfileScreenContent(state = ProfileFixtures.confirmingSignOutState(), onAction = {})
        }

        onNodeWithTag(ProfileTestTags.SIGN_OUT_DIALOG).assertExists()
        onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertExists()
        onNodeWithTag(ProfileTestTags.CONNECTION_CARD, useUnmergedTree = true).assertExists()
    }

    @Test
    fun aRetriableErrorOffersRetry() = runComposeUiTest {
        val actions = mutableListOf<ProfileAction>()
        setContent {
            ProfileScreenContent(
                state = ProfileState(
                    accountId = ProfileFixtures.ACCOUNT_ID,
                    uiState = ProfileUiState.Error(ProfileErrorKind.TokenExpired),
                ),
                onAction = { actions.add(it) },
            )
        }

        onNodeWithTag(ProfileTestTags.RETRY_BUTTON).performClick()

        assertTrue(actions.contains(ProfileAction.RetryLoad))
    }

    @Test
    fun aNonRetriableErrorWithholdsRetry() = runComposeUiTest {
        setContent {
            ProfileScreenContent(
                state = ProfileState(
                    accountId = ProfileFixtures.ACCOUNT_ID,
                    uiState = ProfileUiState.Error(ProfileErrorKind.ProfileNotFound),
                ),
                onAction = {},
            )
        }

        onNodeWithTag(ProfileTestTags.ERROR_STATE).assertExists()
        onNodeWithTag(ProfileTestTags.RETRY_BUTTON).assertDoesNotExist()
    }
}
