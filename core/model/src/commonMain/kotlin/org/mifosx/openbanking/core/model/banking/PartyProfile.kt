/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking

import kotlinx.serialization.Serializable

/**
 * The connected party's identity, derived from OBIE `GET /accounts/{AccountId}/party`.
 *
 * Every field is a display-ready string rather than the raw OBIE shape. The bank sends each
 * component optionally and in several combinations, so the resolution rules — which name wins,
 * how initials are built, how a multi-part address collapses to one line — are applied once in
 * the mapper and travel with the model. Two consumers deriving them independently is how the
 * avatar and the header end up disagreeing.
 *
 * Blank rather than null is used for absent values so the UI branches on `isNotBlank()` alone.
 *
 * @property partyId OBIE `PartyId`. Empty when the payload carried none.
 * @property displayName `Name`, falling back to `FullLegalName`. Drives the header and [initials].
 * @property roleLabel Human-readable account role, resolved from `PartyType` + `AccountRole`.
 * @property initials Up to two uppercase letters from [displayName], for the avatar.
 * @property email OBIE `EmailAddress`.
 * @property mobile OBIE `Mobile`, falling back to `Phone`.
 * @property addressLine The first address collapsed to a single line. **The HSBC UK Personal
 *   sandbox does not return `Address`**, so this is blank there and the row is hidden.
 */
@Serializable
data class PartyProfile(
    val partyId: String,
    val displayName: String,
    val roleLabel: String,
    val initials: String,
    val email: String,
    val mobile: String,
    val addressLine: String,
) {
    /**
     * True when the bank answered without any identity worth rendering — the screen's empty state.
     *
     * Keyed on [displayName] alone: a party with a name but no contact details is a legitimate
     * profile that should render, whereas a name-less payload has nothing to show a user.
     */
    val isEmpty: Boolean get() = displayName.isBlank()
}
