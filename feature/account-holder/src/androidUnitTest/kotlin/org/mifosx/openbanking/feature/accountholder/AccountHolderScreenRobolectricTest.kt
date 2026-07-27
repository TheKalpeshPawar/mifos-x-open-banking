/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderAction
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderErrorKind
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderState
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderUiState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [AccountHolderScreenContent] across its states under Robolectric (JVM, no device) and
 * drives it through the shared [AccountHolderTestTags]. A verbatim on-device mirror lives in
 * [AccountHolderScreenInstrumentedTest]. Identity only — no consent or sign-out surfaces.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class AccountHolderScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<AccountHolderAction>()

    private fun render(state: AccountHolderState) {
        composeRule.setContent {
            AccountHolderScreenContent(state = state, onAction = { actions.add(it) })
        }
    }

    private fun errorState(kind: AccountHolderErrorKind) = AccountHolderState(
        accountId = AccountHolderFixtures.ACCOUNT_ID,
        uiState = AccountHolderUiState.Error(kind),
    )

    @Test
    fun loadingStateRendersTheSpinnerAndCaptionNotContent() {
        render(
            AccountHolderState(
                accountId = AccountHolderFixtures.ACCOUNT_ID,
                uiState = AccountHolderUiState.Loading,
            ),
        )

        composeRule.onNodeWithTag(AccountHolderTestTags.LOADING).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.LOADING_CAPTION, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.CONTENT).assertDoesNotExist()
        composeRule.onNodeWithTag(AccountHolderTestTags.IDENTITY_CARD).assertDoesNotExist()
    }

    @Test
    fun contentStateRendersTheAvatarInitialsNameAndRole() {
        render(AccountHolderFixtures.contentState())

        composeRule.onNodeWithTag(AccountHolderTestTags.IDENTITY_CARD).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.AVATAR, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.DISPLAY_NAME, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.ROLE_LABEL, useUnmergedTree = true).assertExists()
    }

    @Test
    fun contentStateRendersEveryIdentityRowTheBankFilledIn() {
        render(AccountHolderFixtures.contentState())

        composeRule.onNodeWithTag(AccountHolderTestTags.EMAIL_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.MOBILE_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.ADDRESS_ROW, useUnmergedTree = true).assertExists()
    }

    /** The HSBC sandbox sends no address, and the row must disappear rather than render blank. */
    @Test
    fun anIdentityRowWithNoValueIsNotRendered() {
        render(
            AccountHolderState(
                accountId = AccountHolderFixtures.ACCOUNT_ID,
                uiState = AccountHolderFixtures.content()
                    .copy(profile = AccountHolderFixtures.priyaWithoutAddress),
            ),
        )

        composeRule.onNodeWithTag(AccountHolderTestTags.EMAIL_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.ADDRESS_ROW, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun emptyStateRendersTitleAndBody() {
        render(AccountHolderFixtures.emptyState())

        composeRule.onNodeWithTag(AccountHolderTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.EMPTY_TITLE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.EMPTY_BODY, useUnmergedTree = true).assertExists()
    }

    @Test
    fun aRetriableErrorShowsRetryAndDispatchesRetryLoad() {
        render(errorState(AccountHolderErrorKind.TokenExpired))

        composeRule.onNodeWithTag(AccountHolderTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.ERROR_BODY, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.RETRY_BUTTON).performClick()

        assertTrue(actions.contains(AccountHolderAction.RetryLoad))
    }

    /** Re-authorisation failures cannot be retried away, so the button is withheld. */
    @Test
    fun aNonRetriableErrorHidesRetry() {
        render(errorState(AccountHolderErrorKind.ConsentMissingParty))

        composeRule.onNodeWithTag(AccountHolderTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.ERROR_BODY, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.RETRY_BUTTON).assertDoesNotExist()
    }
}
