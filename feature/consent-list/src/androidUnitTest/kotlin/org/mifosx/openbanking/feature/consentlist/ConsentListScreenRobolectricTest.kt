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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListAction
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListErrorKind
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [ConsentListScreenContent] across its states under Robolectric (JVM, no device) and drives
 * it through the shared [ConsentListTestTags]. A verbatim on-device mirror lives in
 * [ConsentListScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class ConsentListScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<ConsentListAction>()
    private var navigatedTo: String? = null
    private var connected = false
    private var reauthenticated = false

    private fun render(state: ConsentListState) {
        composeRule.setContent {
            ConsentListScreenContent(
                state = state,
                onAction = { actions.add(it) },
                onNavigateToDetail = { navigatedTo = it },
                onConnectBank = { connected = true },
                onReauthenticate = { reauthenticated = true },
            )
        }
    }

    @Test
    fun contentRendersTheCurrentConsentCard() {
        render(ConsentListFixtures.contentState())

        composeRule.onNodeWithTag(ConsentListTestTags.CONTENT_LIST).assertExists()
        composeRule.onNodeWithTag(ConsentListTestTags.ACTIVE_SECTION_LABEL).assertExists()
        composeRule.onNodeWithTag(ConsentListTestTags.card(ConsentListFixtures.NEAR_EXPIRY_ID)).assertExists()
    }

    @Test
    fun tappingTheCardRoutesItsConsentId() {
        render(ConsentListFixtures.contentState())

        composeRule.onNodeWithTag(ConsentListTestTags.card(ConsentListFixtures.NEAR_EXPIRY_ID)).performClick()

        assertEquals(ConsentListFixtures.NEAR_EXPIRY_ID, navigatedTo)
    }

    @Test
    fun theUrgencyChipRendersForTheNearExpiryConsent() {
        render(ConsentListFixtures.contentState())

        composeRule.onNodeWithTag(ConsentListTestTags.RECONFIRM_BANNER).assertExists()
        composeRule.onNodeWithTag(
            ConsentListTestTags.urgencyChip(ConsentListFixtures.NEAR_EXPIRY_ID),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun theComfortableConsentShowsNoBanner() {
        render(ConsentListFixtures.activeOnlyState())

        composeRule.onNodeWithTag(ConsentListTestTags.RECONFIRM_BANNER).assertDoesNotExist()
        composeRule.onNodeWithTag(
            ConsentListTestTags.urgencyChip(ConsentListFixtures.ACTIVE_ID),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSpinnerNotTheList() {
        render(ConsentListFixtures.loadingState())

        composeRule.onNodeWithTag(ConsentListTestTags.LOADING).assertExists()
        composeRule.onNodeWithTag(ConsentListTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyOffersConnectAndRaisesTheHostRoute() {
        render(ConsentListFixtures.emptyState())

        composeRule.onNodeWithTag(ConsentListTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(ConsentListTestTags.CONNECT_BUTTON).performClick()

        assertTrue(connected)
    }

    @Test
    fun theGenericErrorDispatchesRetryLoad() {
        render(ConsentListFixtures.errorState(ConsentListErrorKind.ServerError))

        composeRule.onNodeWithTag(ConsentListTestTags.REAUTH_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(ConsentListTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<ConsentListAction>(ConsentListAction.RetryLoad), actions)
    }

    @Test
    fun theExpiredSessionStateRaisesSignInAndOffersNoRetry() {
        render(ConsentListFixtures.authErrorState())

        composeRule.onNodeWithTag(ConsentListTestTags.RETRY_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(ConsentListTestTags.REAUTH_BUTTON).performClick()

        assertTrue(reauthenticated)
        assertTrue(actions.isEmpty())
    }
}
