/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.obp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OBP bank directory entry. Used to resolve a bank id to its display name and country. */
@Serializable
data class Bank(
    val id: String = "",
    @SerialName("short_name") val shortName: String = "",
    @SerialName("full_name") val fullName: String = "",
    val logo: String = "",
    val website: String = "",
    @SerialName("bank_routings") val bankRoutings: List<AccountRouting> = emptyList(),
    val attributes: List<BankAttribute> = emptyList(),
) {
    /** BIC from a bank routing when present, else the SWIFT_BIC bank attribute. */
    val swiftBic: String
        get() = bankRoutings.firstOrNull { it.scheme.equals("BIC", ignoreCase = true) }?.address
            ?: attributes.firstOrNull { it.name.equals("SWIFT_BIC", ignoreCase = true) }?.value.orEmpty()

    /** ISO country code derived from BIC positions 5-6 (e.g. ACMEGB2L -> GB), or "" when unknown. */
    val countryCode: String
        get() = swiftBic.takeIf { it.length >= COUNTRY_END }
            ?.substring(COUNTRY_START, COUNTRY_END)
            ?.uppercase()
            .orEmpty()

    private companion object {
        const val COUNTRY_START = 4
        const val COUNTRY_END = 6
    }
}

/** Name/value attribute attached to an OBP bank (e.g. SWIFT_BIC, REGULATOR). */
@Serializable
data class BankAttribute(
    val name: String = "",
    val value: String = "",
)
