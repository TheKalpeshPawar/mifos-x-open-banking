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

import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.feature.profile.ui.ProfileConnectionUi
import org.mifosx.openbanking.feature.profile.ui.ProfilePermissionUi
import org.mifosx.openbanking.feature.profile.ui.ProfileState
import org.mifosx.openbanking.feature.profile.ui.ProfileUiState
import org.mifosx.openbanking.feature.profile.ui.profilePermissions

/**
 * The profile demo fixture, mirroring `idea-layer/screens/profile/demo-data.yaml`.
 *
 * Shared by the view-model and Compose suites so both assert against the identity the design was
 * drawn against.
 */
object ProfileFixtures {

    const val ACCOUNT_ID: String = "40051512345678"
    const val EXPIRY_LABEL: String = "26 Sep 2026"
    const val EXPIRING_LABEL: String = "04 Jul 2026"
    const val EXPIRING_DAYS: Int = 5

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

    fun permissions(): List<ProfilePermissionUi> = profilePermissions()

    fun connection(
        status: ConsentStatus = ConsentStatus.Authorised,
        expiryLabel: String = EXPIRY_LABEL,
    ): ProfileConnectionUi = ProfileConnectionUi(status = status, expiryLabel = expiryLabel)

    fun content(): ProfileUiState.Content = ProfileUiState.Content(
        profile = priya,
        connection = connection(),
        permissions = permissions(),
        arePermissionsExpanded = false,
        isExpiring = false,
        daysRemaining = Int.MAX_VALUE,
    )

    /** The permissions list opened, for the suites that assert its rows. */
    fun contentPermissionsExpanded(): ProfileUiState.Content =
        content().copy(arePermissionsExpanded = true)

    fun expiring(): ProfileUiState.Content = content().copy(
        connection = connection(expiryLabel = EXPIRING_LABEL),
        isExpiring = true,
        daysRemaining = EXPIRING_DAYS,
    )

    fun contentState(): ProfileState =
        ProfileState(accountId = ACCOUNT_ID, uiState = content())

    fun contentPermissionsExpandedState(): ProfileState =
        ProfileState(accountId = ACCOUNT_ID, uiState = contentPermissionsExpanded())

    fun expiringState(): ProfileState =
        ProfileState(accountId = ACCOUNT_ID, uiState = expiring())

    fun confirmingSignOutState(): ProfileState = ProfileState(
        accountId = ACCOUNT_ID,
        uiState = content(),
        isConfirmingSignOut = true,
    )

    /** The error an expired consent produces, where sign-out must still be reachable. */
    fun errorState(
        kind: org.mifosx.openbanking.feature.profile.ui.ProfileErrorKind =
            org.mifosx.openbanking.feature.profile.ui.ProfileErrorKind.TokenExpired,
    ): ProfileState = ProfileState(
        accountId = ACCOUNT_ID,
        uiState = ProfileUiState.Error(kind),
    )
}
