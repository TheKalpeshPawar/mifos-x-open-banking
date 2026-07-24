/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentlist

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListErrorKind
import kotlin.test.Test

/**
 * Headless smoke coverage for [ConsentListScreenContent] on the desktop renderer, over the same
 * [ConsentListTestTags] the Robolectric and instrumented suites drive.
 *
 * This class lives in `commonTest`, so it also compiles into `androidUnitTest`, where there is no
 * Robolectric runner; the module's build script filters it out of the JVM unit-test tasks.
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native.
 */
@OptIn(ExperimentalTestApi::class)
class ConsentListScreenUiTest {

    private fun androidx.compose.ui.test.ComposeUiTest.render(
        state: org.mifosx.openbanking.feature.consentlist.ui.ConsentListState,
    ) {
        setContent {
            ConsentListScreenContent(
                state = state,
                onAction = {},
                onNavigateToDetail = {},
                onConnectBank = {},
                onReauthenticate = {},
            )
        }
    }

    @Test
    fun contentRendersTheCurrentConsentCard() = runComposeUiTest {
        render(ConsentListFixtures.contentState())

        onNodeWithTag(ConsentListTestTags.CONTENT_LIST).assertExists()
        onNodeWithTag(ConsentListTestTags.ACTIVE_SECTION_LABEL).assertExists()
        onNodeWithTag(ConsentListTestTags.card(ConsentListFixtures.NEAR_EXPIRY_ID)).assertExists()
    }

    @Test
    fun theBannerAndUrgencyChipRenderForANearExpiryConsent() = runComposeUiTest {
        render(ConsentListFixtures.contentState())

        onNodeWithTag(ConsentListTestTags.RECONFIRM_BANNER).assertExists()
        onNodeWithTag(
            ConsentListTestTags.urgencyChip(ConsentListFixtures.NEAR_EXPIRY_ID),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun theComfortableConsentShowsNoBanner() = runComposeUiTest {
        render(ConsentListFixtures.activeOnlyState())

        onNodeWithTag(ConsentListTestTags.RECONFIRM_BANNER).assertDoesNotExist()
        onNodeWithTag(ConsentListTestTags.ACTIVE_SECTION_LABEL).assertExists()
        onNodeWithTag(
            ConsentListTestTags.urgencyChip(ConsentListFixtures.ACTIVE_ID),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSpinnerNotTheList() = runComposeUiTest {
        render(ConsentListFixtures.loadingState())

        onNodeWithTag(ConsentListTestTags.LOADING).assertExists()
        onNodeWithTag(ConsentListTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyRendersTheConnectCallToAction() = runComposeUiTest {
        render(ConsentListFixtures.emptyState())

        onNodeWithTag(ConsentListTestTags.EMPTY_STATE).assertExists()
        onNodeWithTag(ConsentListTestTags.EMPTY_TITLE).assertExists()
        onNodeWithTag(ConsentListTestTags.CONNECT_BUTTON).assertExists()
        onNodeWithTag(ConsentListTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun theGenericErrorOffersRetryAndNotTheSignInRoute() = runComposeUiTest {
        render(ConsentListFixtures.errorState(ConsentListErrorKind.ServerError))

        onNodeWithTag(ConsentListTestTags.ERROR_STATE).assertExists()
        onNodeWithTag(ConsentListTestTags.RETRY_BUTTON).assertExists()
        onNodeWithTag(ConsentListTestTags.REAUTH_BUTTON).assertDoesNotExist()
        onNodeWithTag(ConsentListTestTags.AUTH_ERROR_STATE).assertDoesNotExist()
    }

    /**
     * The expired-session state is a distinct screen, not a variant of the generic error: it offers
     * no Retry, because retrying an expired session just fails again.
     */
    @Test
    fun theExpiredSessionStateOffersSignInAndNoRetry() = runComposeUiTest {
        render(ConsentListFixtures.authErrorState())

        onNodeWithTag(ConsentListTestTags.AUTH_ERROR_STATE).assertExists()
        onNodeWithTag(ConsentListTestTags.AUTH_ERROR_TITLE).assertExists()
        onNodeWithTag(ConsentListTestTags.REAUTH_BUTTON).assertExists()
        onNodeWithTag(ConsentListTestTags.RETRY_BUTTON).assertDoesNotExist()
        onNodeWithTag(ConsentListTestTags.ERROR_STATE).assertDoesNotExist()
    }
}
