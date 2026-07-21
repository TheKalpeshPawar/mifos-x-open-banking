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

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.hsbcPermission.PermissionId
import org.mifosx.openbanking.feature.profile.ui.ProfileAction
import org.mifosx.openbanking.feature.profile.ui.ProfileErrorKind
import org.mifosx.openbanking.feature.profile.ui.ProfileState
import org.mifosx.openbanking.feature.profile.ui.ProfileUiState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [ProfileScreenContent] across its states under Robolectric (JVM, no device) and drives it
 * through the shared [ProfileTestTags]. A verbatim on-device mirror lives in
 * [ProfileScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class ProfileScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<ProfileAction>()

    private fun render(state: ProfileState) {
        composeRule.setContent {
            ProfileScreenContent(state = state, onAction = { actions.add(it) })
        }
    }

    private fun errorState(kind: ProfileErrorKind) = ProfileState(
        accountId = ProfileFixtures.ACCOUNT_ID,
        uiState = ProfileUiState.Error(kind),
    )

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag(ProfileTestTags.CONTENT)
            .performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun loadingStateRendersTheSpinnerAndCaptionNotContent() {
        render(
            ProfileState(
                accountId = ProfileFixtures.ACCOUNT_ID,
                uiState = ProfileUiState.Loading,
            ),
        )

        composeRule.onNodeWithTag(ProfileTestTags.LOADING).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.LOADING_CAPTION, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.CONTENT).assertDoesNotExist()
        composeRule.onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertDoesNotExist()
    }

    @Test
    fun contentStateRendersTheAvatarInitialsNameAndRole() {
        render(ProfileFixtures.contentState())

        composeRule.onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.AVATAR, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.DISPLAY_NAME, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.ROLE_LABEL, useUnmergedTree = true).assertExists()
    }

    @Test
    fun contentStateRendersEveryIdentityRowTheBankFilledIn() {
        render(ProfileFixtures.contentState())

        composeRule.onNodeWithTag(ProfileTestTags.EMAIL_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.MOBILE_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.ADDRESS_ROW, useUnmergedTree = true).assertExists()
    }

    /** The HSBC sandbox sends no address, and the row must disappear rather than render blank. */
    @Test
    fun anIdentityRowWithNoValueIsNotRendered() {
        render(
            ProfileState(
                accountId = ProfileFixtures.ACCOUNT_ID,
                uiState = ProfileFixtures.content()
                    .copy(profile = ProfileFixtures.priyaWithoutAddress),
            ),
        )

        composeRule.onNodeWithTag(ProfileTestTags.EMAIL_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.ADDRESS_ROW, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun theConnectionCardRendersTheBankStatusAndExpiryRows() {
        render(ProfileFixtures.contentState())

        composeRule.onNodeWithTag(ProfileTestTags.CONNECTION_CARD, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.CONNECTION_BANK_ROW, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.STATUS_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.EXPIRY_ROW, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theConnectionCardRendersExactlyFourPermissionRows() {
        render(ProfileFixtures.contentState())

        composeRule.onNodeWithTag(ProfileTestTags.PERMISSIONS_HEADER, useUnmergedTree = true)
            .assertExists()
        ProfileFixtures.permissions().forEach { permission ->
            composeRule.onNodeWithTag(
                ProfileTestTags.permissionRow(permission.id),
                useUnmergedTree = true,
            ).assertExists()
        }
        assertEquals(EXPECTED_PERMISSION_COUNT, ProfileFixtures.permissions().size)
        composeRule.onNodeWithTag(
            ProfileTestTags.permissionRow(PermissionId.ReadDirectDebits),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    /** A healthy consent must not raise the warning strip. */
    @Test
    fun contentRendersNoExpiryBanner() {
        render(ProfileFixtures.contentState())

        composeRule.onNodeWithTag(ProfileTestTags.EXPIRY_BANNER, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(ProfileTestTags.RENEW_CONSENT_BUTTON, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun expiringContentRendersTheBannerBeneathTheConnectionHeader() {
        render(ProfileFixtures.expiringState())

        composeRule.onNodeWithTag(ProfileTestTags.CONNECTION_HEADER, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.EXPIRY_BANNER, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.RENEW_CONSENT_BUTTON, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun expiringContentStillRendersTheIdentityAndPermissionRows() {
        render(ProfileFixtures.expiringState())

        composeRule.onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.EMAIL_ROW, useUnmergedTree = true).assertExists()
        ProfileFixtures.permissions().forEach { permission ->
            composeRule.onNodeWithTag(
                ProfileTestTags.permissionRow(permission.id),
                useUnmergedTree = true,
            ).assertExists()
        }
    }

    @Test
    fun theExpiryBannerNamesTheDayCount() {
        render(ProfileFixtures.expiringState())

        composeRule.onNodeWithTag(ProfileTestTags.EXPIRY_BANNER_BODY, useUnmergedTree = true)
            .assertTextContains(ProfileFixtures.EXPIRING_DAYS.toString(), substring = true)
    }

    @Test
    fun tappingSignOutDispatchesRequestSignOut() {
        render(ProfileFixtures.contentState())

        scrollTo(ProfileTestTags.SIGN_OUT_BUTTON)
        composeRule.onNodeWithTag(ProfileTestTags.SIGN_OUT_BUTTON).performClick()

        assertTrue(actions.contains(ProfileAction.RequestSignOut))
    }

    /**
     * The preview truncates the page behind the scrim, but that is a render shortcut: content that
     * vanishes when a confirmation opens leaves the user nothing to check the decision against.
     */
    @Test
    fun confirmSignOutRendersTheFullContentTreeBehindTheScrim() {
        render(ProfileFixtures.confirmingSignOutState())

        composeRule.onNodeWithTag(ProfileTestTags.SIGN_OUT_DIALOG).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.IDENTITY_CARD).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.EMAIL_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.CONNECTION_CARD, useUnmergedTree = true)
            .assertExists()
        ProfileFixtures.permissions().forEach { permission ->
            composeRule.onNodeWithTag(
                ProfileTestTags.permissionRow(permission.id),
                useUnmergedTree = true,
            ).assertExists()
        }
    }

    @Test
    fun theDialogConfirmAndCancelButtonsDispatchTheirActions() {
        render(ProfileFixtures.confirmingSignOutState())

        composeRule.onNodeWithTag(ProfileTestTags.SIGN_OUT_CANCEL_BUTTON).performClick()
        composeRule.onNodeWithTag(ProfileTestTags.SIGN_OUT_CONFIRM_BUTTON).performClick()

        assertTrue(actions.contains(ProfileAction.DismissSignOut))
        assertTrue(actions.contains(ProfileAction.ConfirmSignOut))
    }

    @Test
    fun tappingManageConsentDispatchesManageConsent() {
        render(ProfileFixtures.contentState())

        scrollTo(ProfileTestTags.MANAGE_CONSENT_BUTTON)
        composeRule.onNodeWithTag(ProfileTestTags.MANAGE_CONSENT_BUTTON).performClick()

        assertTrue(actions.contains(ProfileAction.ManageConsent))
    }

    @Test
    fun emptyStateRendersTheReauthoriseButton() {
        render(
            ProfileState(accountId = ProfileFixtures.ACCOUNT_ID, uiState = ProfileUiState.Empty),
        )

        composeRule.onNodeWithTag(ProfileTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.EMPTY_TITLE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.EMPTY_BODY, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.REAUTHORISE_BUTTON).performClick()

        assertTrue(actions.contains(ProfileAction.Reauthorise))
    }

    @Test
    fun aRetriableErrorShowsRetryAndDispatchesRetryLoad() {
        render(errorState(ProfileErrorKind.TokenExpired))

        composeRule.onNodeWithTag(ProfileTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.ERROR_BODY, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.RETRY_BUTTON).performClick()

        assertTrue(actions.contains(ProfileAction.RetryLoad))
    }

    /** Re-authorisation failures cannot be retried away, so the button is withheld. */
    @Test
    fun aNonRetriableErrorHidesRetry() {
        render(errorState(ProfileErrorKind.ConsentMissingParty))

        composeRule.onNodeWithTag(ProfileTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.ERROR_BODY, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ProfileTestTags.RETRY_BUTTON).assertDoesNotExist()
    }

    private companion object {
        const val EXPECTED_PERMISSION_COUNT = 4
    }
}
