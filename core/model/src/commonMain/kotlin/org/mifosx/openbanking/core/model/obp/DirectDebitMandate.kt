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

import kotlinx.serialization.Serializable

/**
 * A direct-debit mandate DERIVED from transaction history. OBP exposes only a
 * POST-create endpoint for direct debits (no list/get/cancel at any API version),
 * so mandates are reconstructed from outgoing `TXN_TYPE=DD` transactions grouped
 * into collection series.
 */
@Serializable
data class DirectDebitMandate(
    val id: String = "",
    /** Who collects — taken from the transaction description, never the holder placeholder. */
    val merchantName: String = "",
    val amountValue: String = "",
    val amountCurrency: String = "",
    val frequency: String = "MONTHLY",
    /** Most recent observed collection date (ISO date, "" when unknown). */
    val lastCollectionDate: String = "",
    /** Next expected collection date (last + period; "" when underivable). */
    val nextCollectionDate: String = "",
    val status: String = STATUS_ACTIVE,
    /** Stable display reference ("DD-{initials}-{first collection yyyyMMdd}"). */
    val mandateReference: String = "",
    /** First observed collection date (ISO date, "" when unknown) — the mandate's effective start. */
    val firstCollectionDate: String = "",
    /** Observed collections for this series, most recent first; bounded to the latest few. */
    val recentCollections: List<DirectDebitCollection> = emptyList(),
) {
    val isActive: Boolean get() = status == STATUS_ACTIVE

    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}

/**
 * One observed direct-debit collection in a mandate's history. Derived from a booked
 * `TXN_TYPE=DD` transaction, so every collection here has settled — [amountValue] is the
 * unsigned magnitude on [date] (ISO date).
 */
@Serializable
data class DirectDebitCollection(
    val date: String = "",
    val amountValue: String = "",
    val amountCurrency: String = "",
)
