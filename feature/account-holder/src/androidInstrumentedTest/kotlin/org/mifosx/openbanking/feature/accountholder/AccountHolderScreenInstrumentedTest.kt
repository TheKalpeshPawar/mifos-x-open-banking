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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderAction
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderState
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderUiState

private const val ACCOUNT_ID = "40051512345678"

/**
 * androidInstrumentedTest is a separate compilation that cannot see commonTest, so `AccountHolderFixtures`
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

private fun content(): AccountHolderUiState.Content = AccountHolderUiState.Content(profile = priya)

private fun contentState(): AccountHolderState =
    AccountHolderState(accountId = ACCOUNT_ID, uiState = content())

/**
 * On-device mirror of [AccountHolderScreenRobolectricTest], driving the same [AccountHolderTestTags] so a
 * divergence between the JVM and device renderers is visible. Identity only — no consent or sign-out.
 */
@RunWith(AndroidJUnit4::class)
class AccountHolderScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<AccountHolderAction>()

    private fun render(state: AccountHolderState) {
        composeRule.setContent {
            AccountHolderScreenContent(state = state, onAction = { actions.add(it) })
        }
    }

    @Test
    fun contentStateRendersTheIdentityCardAndRows() {
        render(contentState())

        composeRule.onNodeWithTag(AccountHolderTestTags.IDENTITY_CARD).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.AVATAR, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.EMAIL_ROW, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AccountHolderTestTags.MOBILE_ROW, useUnmergedTree = true).assertExists()
    }

    @Test
    fun anIdentityRowWithNoValueIsNotRendered() {
        render(
            AccountHolderState(
                accountId = ACCOUNT_ID,
                uiState = content().copy(profile = priya.copy(addressLine = "")),
            ),
        )

        composeRule.onNodeWithTag(AccountHolderTestTags.ADDRESS_ROW, useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
