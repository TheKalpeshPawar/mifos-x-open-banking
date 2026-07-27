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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderAction
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderErrorKind
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderState
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderUiState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Headless smoke pass over [AccountHolderScreenContent], covering the state fork itself rather than
 * the detail of each surface — that is what the Robolectric and instrumented suites are for.
 *
 * Lives in commonTest so it compiles for every target, and is excluded from the Android unit-test
 * tasks by the module's build script: there is no Robolectric runner there to supply an Android
 * runtime, so every case would NPE.
 */
@OptIn(ExperimentalTestApi::class)
class AccountHolderScreenUiTest {

    @Test
    fun loadingRendersTheSpinner() = runComposeUiTest {
        setContent {
            AccountHolderScreenContent(
                state = AccountHolderState(
                    accountId = AccountHolderFixtures.ACCOUNT_ID,
                    uiState = AccountHolderUiState.Loading,
                ),
                onAction = {},
            )
        }

        onNodeWithTag(AccountHolderTestTags.LOADING).assertIsDisplayed()
        onNodeWithTag(AccountHolderTestTags.CONTENT).assertDoesNotExist()
    }

    @Test
    fun contentRendersTheIdentity() = runComposeUiTest {
        setContent { AccountHolderScreenContent(state = AccountHolderFixtures.contentState(), onAction = {}) }

        onNodeWithTag(AccountHolderTestTags.IDENTITY_CARD).assertExists()
        onNodeWithTag(AccountHolderTestTags.IDENTITY_SECTION).assertExists()
    }

    @Test
    fun emptyRendersTheEmptyState() = runComposeUiTest {
        setContent { AccountHolderScreenContent(state = AccountHolderFixtures.emptyState(), onAction = {}) }

        onNodeWithTag(AccountHolderTestTags.EMPTY_STATE).assertExists()
        onNodeWithTag(AccountHolderTestTags.CONTENT).assertDoesNotExist()
    }

    @Test
    fun aRetriableErrorOffersRetry() = runComposeUiTest {
        val actions = mutableListOf<AccountHolderAction>()
        setContent {
            AccountHolderScreenContent(
                state = AccountHolderState(
                    accountId = AccountHolderFixtures.ACCOUNT_ID,
                    uiState = AccountHolderUiState.Error(AccountHolderErrorKind.TokenExpired),
                ),
                onAction = { actions.add(it) },
            )
        }

        onNodeWithTag(AccountHolderTestTags.RETRY_BUTTON).performClick()

        assertTrue(actions.contains(AccountHolderAction.RetryLoad))
    }

    @Test
    fun aNonRetriableErrorWithholdsRetry() = runComposeUiTest {
        setContent {
            AccountHolderScreenContent(
                state = AccountHolderState(
                    accountId = AccountHolderFixtures.ACCOUNT_ID,
                    uiState = AccountHolderUiState.Error(AccountHolderErrorKind.ProfileNotFound),
                ),
                onAction = {},
            )
        }

        onNodeWithTag(AccountHolderTestTags.ERROR_STATE).assertExists()
        onNodeWithTag(AccountHolderTestTags.RETRY_BUTTON).assertDoesNotExist()
    }
}
