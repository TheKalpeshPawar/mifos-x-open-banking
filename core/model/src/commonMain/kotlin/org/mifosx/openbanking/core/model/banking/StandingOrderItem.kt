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
 * One standing order, derived from OBIE `GET /accounts/{AccountId}/standing-orders`.
 *
 * Inactive orders are modelled alongside active ones rather than filtered out: the bank returns
 * both and the screen renders both, dimmed, so a user can see a cancelled instruction is genuinely
 * gone rather than merely missing from the list.
 *
 * Amounts and dates stay as the raw OBIE strings. Currency symbols and `1 Jul 2026` formatting are
 * display concerns and belong in the view model.
 *
 * [frequencyLabel] is the one exception: decoding the ISO 20022 interval code needs the OBIE
 * vocabulary, which is a payload concern rather than a presentation one, so it is resolved here
 * once instead of in every consumer.
 *
 * @property standingOrderId OBIE `StandingOrderId`. Empty string when the payload carried none.
 * @property payeeName Beneficiary name from `CreditorAccount.Name`, e.g. `Jameson Lettings`.
 * @property statusCode OBIE `StandingOrderStatusCode` verbatim, e.g. `Active` / `Inactive`.
 * @property isActive Whether [statusCode] reads `Active`. Drives badge styling, the summary counts
 *   and the sort order, so it is resolved once at the mapper rather than re-derived per consumer.
 * @property nextPaymentAmount Raw OBIE `NextPaymentAmount.Amount` decimal string.
 * @property currency ISO-4217 code from `NextPaymentAmount.Currency`.
 * @property frequencyLabel Human-readable interval, e.g. `Monthly on the 1st`. Falls back to the
 *   raw OBIE code when the composite key is not one the decoder knows.
 * @property nextPaymentDateTime ISO-8601 `NextPaymentDateTime`; empty when the order has no
 *   scheduled next payment, which is normal for a cancelled one.
 * @property finalPaymentDateTime ISO-8601 `MandateRelatedInformation.FinalPaymentDateTime`; empty
 *   when the order runs indefinitely.
 * @property hasFinalPayment Whether [finalPaymentDateTime] carries a value. Kept explicit because
 *   the card shows or omits a whole line on it.
 * @property creditorIdentification `CreditorAccount.Identification`, the sort code and account
 *   number the payment leaves for.
 * @property reference OBIE `Reference`, e.g. `RENT-FLAT12` — what distinguishes two orders to the
 *   same payee.
 */
@Serializable
data class StandingOrderItem(
    val standingOrderId: String,
    val payeeName: String,
    val statusCode: String,
    val isActive: Boolean,
    val nextPaymentAmount: String,
    val currency: String,
    val frequencyLabel: String,
    val nextPaymentDateTime: String,
    val finalPaymentDateTime: String,
    val hasFinalPayment: Boolean,
    val creditorIdentification: String,
    val reference: String,
)

/**
 * The standing order list together with the two figures the summary row reads out.
 *
 * The counts travel with the list rather than being recomputed by each consumer: they are a
 * property of one fetched payload, and deriving them twice invites the summary and the list
 * disagreeing after a partial update.
 *
 * @property items Orders sorted active-first, matching the rendered order.
 * @property activeCount Number of orders whose status reads `Active`.
 * @property inactiveCount Number of orders whose status does not read `Active`.
 */
@Serializable
data class StandingOrdersSummary(
    val items: List<StandingOrderItem>,
    val activeCount: Int,
    val inactiveCount: Int,
) {
    /** True when the bank returned no standing orders at all — the screen's empty state. */
    val isEmpty: Boolean get() = items.isEmpty()
}
