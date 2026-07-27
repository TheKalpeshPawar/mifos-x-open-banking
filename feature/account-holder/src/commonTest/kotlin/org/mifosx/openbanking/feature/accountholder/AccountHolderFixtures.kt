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

import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderErrorKind
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderState
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderUiState

/**
 * The account-holder demo fixture — the party identity for one account.
 *
 * Shared by the view-model and Compose suites so both assert against the identity the design was
 * drawn against.
 */
object AccountHolderFixtures {

    const val ACCOUNT_ID: String = "40051512345678"

    val priya: PartyProfile = PartyProfile(
        partyId = "PTY-1029384756",
        displayName = "Priya Sharma",
        roleLabel = "Personal Account Holder",
        initials = "PS",
        email = "priya.sharma@example.co.uk",
        mobile = "+44 7700 900482",
        addressLine = "12 Baker Street, London W1U 6TZ",
    )

    /**
     * What the HSBC UK Personal sandbox actually returns: no `Address`, so that row must hide.
     */
    val priyaWithoutAddress: PartyProfile = priya.copy(addressLine = "")

    val nameless: PartyProfile = PartyProfile(
        partyId = "",
        displayName = "",
        roleLabel = "",
        initials = "",
        email = "",
        mobile = "",
        addressLine = "",
    )

    fun content(): AccountHolderUiState.Content = AccountHolderUiState.Content(profile = priya)

    fun contentState(): AccountHolderState =
        AccountHolderState(accountId = ACCOUNT_ID, uiState = content())

    fun emptyState(): AccountHolderState =
        AccountHolderState(accountId = ACCOUNT_ID, uiState = AccountHolderUiState.Empty)

    fun errorState(
        kind: AccountHolderErrorKind = AccountHolderErrorKind.TokenExpired,
    ): AccountHolderState = AccountHolderState(
        accountId = ACCOUNT_ID,
        uiState = AccountHolderUiState.Error(kind),
    )
}
