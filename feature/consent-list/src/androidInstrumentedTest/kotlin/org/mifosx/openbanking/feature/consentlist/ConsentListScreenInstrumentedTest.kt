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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.feature.consentlist.ui.ConsentCardUi
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListAction
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListErrorKind
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListState
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListUiState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ACTIVE_ID = "aac-fb2c4e8a-7d31-4c9e-9f2a-1b3c5d7e9f01"
private const val NEAR_EXPIRY_ID = "aac-d4e5f6a7-8b9c-4d0e-1f2a-3b4c5d6e7f08"
private const val ACTIVE_DAYS = 89
private const val NEAR_EXPIRY_DAYS = 7
private const val ACTIVE_PERMISSIONS = 10

/**
 * androidInstrumentedTest does not see commonTest, so the state fixtures are inlined here.
 */
private fun activeCard() = ConsentCardUi(
    consentId = ACTIVE_ID,
    status = ConsentStatus.Authorised,
    permissionCount = ACTIVE_PERMISSIONS,
    daysUntilExpiry = ACTIVE_DAYS,
    expiredOnDate = null,
    connectedDate = "28 Jun 2026",
    isNearExpiry = false,
)

private fun nearExpiryCard() = ConsentCardUi(
    consentId = NEAR_EXPIRY_ID,
    status = ConsentStatus.Authorised,
    permissionCount = 4,
    daysUntilExpiry = NEAR_EXPIRY_DAYS,
    expiredOnDate = null,
    connectedDate = "7 Apr 2026",
    isNearExpiry = true,
)

private fun contentState() = ConsentListState(
    uiState = ConsentListUiState.Content(
        active = listOf(nearExpiryCard()),
        showReconfirmBanner = true,
    ),
)

private fun comfortableState() = ConsentListState(
    uiState = ConsentListUiState.Content(
        active = listOf(activeCard()),
        showReconfirmBanner = false,
    ),
)

private fun loadingState() = ConsentListState(uiState = ConsentListUiState.Loading)

private fun emptyState() = ConsentListState(uiState = ConsentListUiState.Empty)

private fun errorState() = ConsentListState(uiState = ConsentListUiState.Error(ConsentListErrorKind.ServerError))

private fun authErrorState() = ConsentListState(uiState = ConsentListUiState.ErrorAuth)

/**
 * On-device mirror of [ConsentListScreenRobolectricTest], driving the same [ConsentListTestTags] so a
 * divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class ConsentListScreenInstrumentedTest {

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
        render(contentState())

        composeRule.onNodeWithTag(ConsentListTestTags.CONTENT_LIST).assertExists()
        composeRule.onNodeWithTag(ConsentListTestTags.ACTIVE_SECTION_LABEL).assertExists()
        composeRule.onNodeWithTag(ConsentListTestTags.card(NEAR_EXPIRY_ID)).assertExists()
    }

    @Test
    fun tappingTheCardRoutesItsConsentId() {
        render(contentState())

        composeRule.onNodeWithTag(ConsentListTestTags.card(NEAR_EXPIRY_ID)).performClick()

        assertEquals(NEAR_EXPIRY_ID, navigatedTo)
    }

    @Test
    fun theUrgencyChipRendersForTheNearExpiryConsent() {
        render(contentState())

        composeRule.onNodeWithTag(ConsentListTestTags.RECONFIRM_BANNER).assertExists()
        composeRule.onNodeWithTag(
            ConsentListTestTags.urgencyChip(NEAR_EXPIRY_ID),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun theComfortableConsentShowsNoBanner() {
        render(comfortableState())

        composeRule.onNodeWithTag(ConsentListTestTags.RECONFIRM_BANNER).assertDoesNotExist()
        composeRule.onNodeWithTag(
            ConsentListTestTags.urgencyChip(ACTIVE_ID),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSpinnerNotTheList() {
        render(loadingState())

        composeRule.onNodeWithTag(ConsentListTestTags.LOADING).assertExists()
        composeRule.onNodeWithTag(ConsentListTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyOffersConnectAndRaisesTheHostRoute() {
        render(emptyState())

        composeRule.onNodeWithTag(ConsentListTestTags.CONNECT_BUTTON).performClick()

        assertTrue(connected)
    }

    @Test
    fun theGenericErrorDispatchesRetryLoad() {
        render(errorState())

        composeRule.onNodeWithTag(ConsentListTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<ConsentListAction>(ConsentListAction.RetryLoad), actions)
    }

    @Test
    fun theExpiredSessionStateRaisesSignInAndOffersNoRetry() {
        render(authErrorState())

        composeRule.onNodeWithTag(ConsentListTestTags.RETRY_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(ConsentListTestTags.REAUTH_BUTTON).performClick()

        assertTrue(reauthenticated)
    }
}
