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

/**
 * A recurring outgoing payment shown on the Standing Orders screen.
 *
 * OBP exposes no read endpoint for standing orders (POST-create only), so rows are
 * DERIVED from the account's transaction history (TXN_TYPE=SO attribute), merged with
 * orders created on this device (persisted locally). Serializable because created
 * orders are cached as JSON in the Room cache.
 */
@Serializable
data class StandingOrder(
    val id: String = "",
    /** Display name — the recurring transaction description or the counterparty name. */
    val name: String = "",
    /** Who gets paid — the counterparty account holder's name ("" when unknown). */
    @SerialName("counterparty_name") val counterpartyName: String = "",
    /** Raw counterparty account/counterparty id ("" when unknown); the UI masks it. */
    @SerialName("counterparty_account") val counterpartyAccount: String = "",
    /** Absolute payment amount, e.g. "45.00". */
    @SerialName("amount_value") val amountValue: String = "",
    @SerialName("amount_currency") val amountCurrency: String = "",
    /** OBP frequency vocabulary: YEARLY / MONTHLY / BI-WEEKLY / WEEKLY / DAILY. */
    val frequency: String = "",
    /** ISO date of the most recent observed payment ("" for created-but-not-yet-run orders). */
    @SerialName("last_payment_date") val lastPaymentDate: String = "",
    /** ISO date the next payment is expected. */
    @SerialName("next_payment_date") val nextPaymentDate: String = "",
    /** [STATUS_ACTIVE], [STATUS_PAUSED] (no payment within 1.5x period) or [STATUS_CANCELLED] (3x). */
    val status: String = STATUS_ACTIVE,
    /** True when this order was created from this device (not derived from history). */
    val created: Boolean = false,
) {
    val isActive: Boolean get() = status == STATUS_ACTIVE
    val isPaused: Boolean get() = status == STATUS_PAUSED
    val isCancelled: Boolean get() = status == STATUS_CANCELLED

    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}

/** Request body for OBP v4.0.0 `POST .../owner/standing-order`. */
@Serializable
data class CreateStandingOrderRequest(
    @SerialName("customer_id") val customerId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("counterparty_id") val counterpartyId: String,
    val amount: AmountOfMoney,
    val `when`: StandingOrderSchedule,
    @SerialName("date_signed") val dateSigned: String,
    @SerialName("date_starts") val dateStarts: String,
    @SerialName("date_expires") val dateExpires: String? = null,
)

/** OBP standing-order schedule: frequency + optional anchor detail (FIRST_DAY / LAST_DAY / FIRST_MONDAY). */
@Serializable
data class StandingOrderSchedule(
    val frequency: String,
    val detail: String = "FIRST_DAY",
)

/** Response body of OBP v4.0.0 `POST .../owner/standing-order`. */
@Serializable
data class CreateStandingOrderResponse(
    @SerialName("standing_order_id") val standingOrderId: String = "",
    @SerialName("bank_id") val bankId: String = "",
    @SerialName("account_id") val accountId: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("counterparty_id") val counterpartyId: String = "",
    val amount: AmountOfMoney = AmountOfMoney(),
    val `when`: StandingOrderSchedule = StandingOrderSchedule(frequency = ""),
    @SerialName("date_signed") val dateSigned: String = "",
    @SerialName("date_starts") val dateStarts: String = "",
    @SerialName("date_expires") val dateExpires: String = "",
    @SerialName("date_cancelled") val dateCancelled: String = "",
    val active: Boolean = false,
)
