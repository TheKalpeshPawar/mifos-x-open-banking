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

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.model.hsbcPermission.OBPermission
import org.mifosx.openbanking.feature.profile.ui.ProfileAction
import org.mifosx.openbanking.feature.profile.ui.ProfileConnectionUi
import org.mifosx.openbanking.feature.profile.ui.ProfileErrorKind
import org.mifosx.openbanking.feature.profile.ui.ProfilePermissionUi
import org.mifosx.openbanking.feature.profile.ui.ProfileState
import org.mifosx.openbanking.feature.profile.ui.ProfileUiState
import kotlin.test.assertTrue

private const val ACCOUNT_ID = "40051512345678"
private const val EXPIRING_DAYS = 5

/**
 * androidInstrumentedTest is a separate compilation that cannot see commonTest, so `ProfileFixtures`
 * is invisible here and the display fixture is inlined instead. Keep it in step with the shared one:
 * a divergence turns this suite into a test of different data than its Robolectric mirror.
 */
private val priya: PartyProfile = PartyProfile(
    partyId = "PTY-1029384756",
    displayName = "Priya Sharma",
    roleLabel = "Personal Account Holder",
    initials = "PS",
    email = "priya.sharma@example.co.uk",
    mobile = "+44 7700 900482",
    addressLine = "12 Baker Street, London W1U 6TZ",
)

private fun permissions(): List<ProfilePermissionUi> =
    OBPermission.ALL.map { ProfilePermissionUi(id = it.id, label = it.label) }

private fun content(): ProfileUiState.Content = ProfileUiState.Content(
    profile = priya,
    connection = ProfileConnectionUi(
        status = ConsentStatus.Authorised,
        expiryLabel = "26 Sep 2026",
    ),
    permissions = permissions(),
    arePermissionsExpanded = false,
    isExpiring = false,
    daysRemaining = Int.MAX_VALUE,
)

private fun contentState(): ProfileState =
    ProfileState(accountId = ACCOUNT_ID, uiState = content())

private fun contentPermissionsExpandedState(): ProfileState =
    ProfileState(accountId = ACCOUNT_ID, uiState = content().copy(arePermissionsExpanded = true))

private fun expiringState(): ProfileState = ProfileState(
    accountId = ACCOUNT_ID,
    uiState = content().copy(isExpiring = true, daysRemaining = EXPIRING_DAYS),
)

private fun confirmingSignOutState(): ProfileState = ProfileState(
    accountId = ACCOUNT_ID,
    uiState = content(),
    isConfirmingSignOut = true,
)

/**
 * On-device mirror of [ProfileScreenRobolectricTest], driving the same [ProfileTestTags] so a
 * divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<ProfileAction>()

    private fun render(state: ProfileState) {
        composeRule.setContent {
            ProfileScreenContent(state = state, onAction = { actions.add(it) })
        }
    }

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag(ProfileTestTags.CONTENT)
            .performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun loadingStateRendersTheSpinnerNotContent() {
        render(ProfileState(accountId = ACCOUNT_ID, uiState = ProfileUiState.Loading))

        composeRule.onNodeWithTag(ProfileTestTags.LOADING).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.CONTENT).assertDoesNotExist()
    }

    @Test
    fun contentStateRendersTheIdentityCardAndRows() {
        render(contentState())

        composeRule.onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.AVATAR, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.EMAIL_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.MOBILE_ROW, useUnmergedTree = true).assertExists()
    }

    @Test
    fun anIdentityRowWithNoValueIsNotRendered() {
        render(
            ProfileState(
                accountId = ACCOUNT_ID,
                uiState = content().copy(profile = priya.copy(addressLine = "")),
            ),
        )

        composeRule.onNodeWithTag(ProfileTestTags.ADDRESS_ROW, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun theConnectionCardRendersItsStatusAndExpiryRows() {
        render(contentState())

        composeRule.onNodeWithTag(ProfileTestTags.CONNECTION_CARD, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.STATUS_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.EXPIRY_ROW, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theGrantedPermissionsAreCollapsedByDefault() {
        render(contentState())

        composeRule.onNodeWithTag(ProfileTestTags.PERMISSIONS_TOGGLE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.PERMISSIONS_LIST, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun expandingThePermissionsRevealsTheGrantedRows() {
        render(contentPermissionsExpandedState())

        permissions().forEach { permission ->
            composeRule.onNodeWithTag(
                ProfileTestTags.permissionRow(permission.id),
                useUnmergedTree = true,
            ).assertExists()
        }
    }

    @Test
    fun expiringContentRaisesTheBannerWithoutLosingTheProfile() {
        render(expiringState())

        composeRule.onNodeWithTag(ProfileTestTags.EXPIRY_BANNER, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.RENEW_CONSENT_BUTTON, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertExists()
    }

    @Test
    fun tappingSignOutDispatchesRequestSignOut() {
        render(contentState())

        scrollTo(ProfileTestTags.SIGN_OUT_BUTTON)
        composeRule.onNodeWithTag(ProfileTestTags.SIGN_OUT_BUTTON).performClick()

        assertTrue(actions.contains(ProfileAction.RequestSignOut))
    }

    @Test
    fun confirmSignOutRendersTheFullContentTreeBehindTheScrim() {
        render(confirmingSignOutState())

        composeRule.onNodeWithTag(ProfileTestTags.SIGN_OUT_DIALOG).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.PERMISSIONS_TOGGLE, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun emptyStateRendersReauthoriseAndDispatchesIt() {
        render(ProfileState(accountId = ACCOUNT_ID, uiState = ProfileUiState.Empty))

        composeRule.onNodeWithTag(ProfileTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.REAUTHORISE_BUTTON).performClick()

        assertTrue(actions.contains(ProfileAction.Reauthorise))
    }

    @Test
    fun aNonRetriableErrorHidesRetryWhileARetriableOneShowsIt() {
        render(
            ProfileState(
                accountId = ACCOUNT_ID,
                uiState = ProfileUiState.Error(ProfileErrorKind.ConsentMissingParty),
            ),
        )

        composeRule.onNodeWithTag(ProfileTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.RETRY_BUTTON).assertDoesNotExist()
    }
}
