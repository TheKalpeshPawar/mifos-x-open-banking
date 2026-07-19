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
 * Client-derived personal-finance category for a transaction row.
 *
 * OBIE `OBTransaction6` does not return a category, so it is inferred from the merchant
 * category code / proprietary bank-transaction code (see the data-layer category mapper).
 * Applies to both debits and credits — a credit can be a refund of a categorised purchase,
 * so the direction is never used to infer the category.
 */
@Serializable
enum class TransactionCategory {
    GROCERIES,
    DINING,
    SUBSCRIPTIONS,
    SHOPPING,
    TRANSPORT,
    TRANSFER,
    OTHER,
}
