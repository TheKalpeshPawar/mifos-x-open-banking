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
 * A single typed balance row from OBIE `GET /accounts/{AccountId}/balances`.
 *
 * OBIE returns one row per balance type (`InterimAvailable`, `InterimBooked`, `OpeningBooked`, …).
 * [AccountBalance] collapses the same payload to the two figures the home hero card shows; this
 * model preserves every row.
 *
 * The amount stays a raw decimal string exactly as OBIE emits it — grouping separators and the
 * currency suffix are applied at the presentation layer.
 *
 * @property type OBIE `Type`, rendered verbatim as the row label (e.g. `InterimAvailable`).
 * @property amount Raw decimal amount string (e.g. `2847.63`).
 * @property currency ISO-4217 currency code for this row.
 * @property dateTime OBIE `DateTime` as an ISO-8601 string; empty when absent.
 */
@Serializable
data class AccountBalanceLine(
    val type: String,
    val amount: String,
    val currency: String,
    val dateTime: String = "",
)
