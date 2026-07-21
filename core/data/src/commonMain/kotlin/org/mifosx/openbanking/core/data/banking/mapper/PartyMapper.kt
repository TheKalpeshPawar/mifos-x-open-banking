/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.core.network.model.ais.party.Party
import org.mifosx.openbanking.core.network.model.ais.party.PartyAddress
import org.mifosx.openbanking.core.network.model.ais.party.PartyResponse

private const val MAX_INITIALS = 2
private const val SOLE_PARTY_TYPE = "Sole"

/**
 * Maps the OBIE party payload to the display-ready [PartyProfile].
 *
 * A response with no `Data.Party` block, or one whose party carries no name, maps to a profile
 * reporting [PartyProfile.isEmpty] rather than throwing: the endpoint answers 200 with an absent
 * party when the consent does not extend to identity, which is a state the screen renders, not a
 * failure it retries.
 */
fun PartyResponse.toPartyProfile(): PartyProfile =
    data?.party.toPartyProfile()

private fun Party?.toPartyProfile(): PartyProfile {
    val resolvedName = this?.resolveName().orEmpty()
    return PartyProfile(
        partyId = this?.partyId.orEmpty(),
        displayName = resolvedName,
        roleLabel = this?.resolveRoleLabel().orEmpty(),
        initials = resolvedName.toInitials(),
        email = this?.emailAddress.orEmpty(),
        mobile = this?.resolveMobile().orEmpty(),
        addressLine = this?.address?.firstOrNull().toAddressLine(),
    )
}

/**
 * `Name` is the bank's preferred display form; `FullLegalName` is the fallback when it is absent.
 */
private fun Party.resolveName(): String =
    name?.takeIf { it.isNotBlank() } ?: fullLegalName.orEmpty()

/**
 * `Mobile` is preferred over `Phone` because the profile row is labelled Mobile; HSBC frequently
 * returns the same number in both.
 */
private fun Party.resolveMobile(): String =
    mobile?.takeIf { it.isNotBlank() } ?: phone.orEmpty()

/**
 * OBIE reports the role as a machine token (`PartyType: Sole`, `AccountRole: UK.OBIE.Principal`).
 * A sole principal is rendered as a personal account holder; anything else falls back to the raw
 * `PartyType` so an unfamiliar structure still says something true rather than nothing.
 */
private fun Party.resolveRoleLabel(): String = when {
    partyType.equals(SOLE_PARTY_TYPE, ignoreCase = true) -> "Personal Account Holder"
    else -> partyType.orEmpty()
}

/**
 * First letter of the first and last name tokens, uppercased — "Priya Sharma" reads "PS".
 * A single-token name yields one letter; a blank name yields none.
 */
private fun String.toInitials(): String {
    val tokens = split(' ', '\t').filter { it.isNotBlank() }
    return when {
        tokens.isEmpty() -> ""
        tokens.size == 1 -> tokens.first().take(1).uppercase()
        else -> (tokens.first().take(1) + tokens.last().take(1)).uppercase()
    }.take(MAX_INITIALS)
}

/**
 * Collapses one structured address to a single display line in OBIE property order, tolerating any
 * combination of present fields. An unstructured `AddressLine` list is used verbatim when the bank
 * sends no structured components.
 */
private fun PartyAddress?.toAddressLine(): String {
    if (this == null) return ""
    val structured = listOfNotNull(
        listOfNotNull(buildingNumber?.takeIf { it.isNotBlank() }, streetName?.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .takeIf { it.isNotBlank() },
        townName?.takeIf { it.isNotBlank() },
        postCode?.takeIf { it.isNotBlank() },
    )
    return structured.takeIf { it.isNotEmpty() }?.joinToString(", ")
        ?: addressLine?.filter { it.isNotBlank() }?.joinToString(", ").orEmpty()
}
