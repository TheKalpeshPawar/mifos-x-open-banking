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
 * One direct debit mandate, derived from OBIE `GET /accounts/{AccountId}/direct-debits`.
 *
 * Inactive mandates are modelled alongside active ones rather than filtered out: the bank returns
 * both and the screen renders both, dimmed, so a user can see a cancelled mandate is genuinely gone
 * rather than merely missing from the list.
 *
 * Amounts stay as the raw OBIE decimal string. Formatting is a display concern and belongs in the
 * view model, which is where the currency symbol and thousands separators are applied.
 *
 * @property mandateId OBIE `MandateRelatedInformation.MandateIdentification` — the reference a user
 *   quotes to the originator or to support. Empty string when the payload carried none.
 * @property name Originator name (OBIE `Name`), e.g. `British Gas`.
 * @property statusCode OBIE `DirectDebitStatusCode` verbatim, e.g. `Active` / `Inactive`.
 * @property isActive Whether [statusCode] reads `Active`. Drives badge styling, the summary counts
 *   and the sort order, so it is resolved once at the mapper rather than re-derived per consumer.
 * @property previousPaymentAmount Raw OBIE `PreviousPaymentAmount.Amount` decimal string.
 * @property currency ISO-4217 code from `PreviousPaymentAmount.Currency`.
 * @property previousPaymentDateTime ISO-8601 `PreviousPaymentDateTime`; formatted for display in
 *   the view model. Empty string when the mandate has never been collected.
 */
@Serializable
data class DirectDebitItem(
    val mandateId: String,
    val name: String,
    val statusCode: String,
    val isActive: Boolean,
    val previousPaymentAmount: String,
    val currency: String,
    val previousPaymentDateTime: String,
)

/**
 * The mandate list together with the two figures the summary chips render.
 *
 * The counts travel with the list rather than being recomputed by each consumer: they are a
 * property of one fetched payload, and deriving them twice invites the chips and the list
 * disagreeing after a partial update.
 *
 * @property items Mandates sorted active-first, matching the rendered order.
 * @property activeCount Number of mandates whose status reads `Active`.
 * @property inactiveCount Number of mandates whose status does not read `Active`.
 */
@Serializable
data class DirectDebitsSummary(
    val items: List<DirectDebitItem>,
    val activeCount: Int,
    val inactiveCount: Int,
) {
    /** True when the bank returned no mandates at all — the screen's empty state. */
    val isEmpty: Boolean get() = items.isEmpty()
}
