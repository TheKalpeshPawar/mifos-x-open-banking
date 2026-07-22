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
 * A single statement's identity, period and money lines, derived from the OBIE `OBStatement2` payload.
 *
 * Every field is a raw OBIE value; the view model formats the ISO-8601 dates and the amount/currency
 * pairs for display. The statement's transactions are modelled separately (as [TransactionItem]s) and
 * merged in the view model, because they come from a distinct endpoint.
 *
 * @property periodStart Raw ISO-8601 `StartDateTime`.
 * @property periodEnd Raw ISO-8601 `EndDateTime`.
 * @property created Raw ISO-8601 `CreationDateTime`.
 * @property balances The `StatementAmount` entries (opening/closing balance).
 * @property fees The `StatementFee` entries, empty when the bank returned none.
 * @property interest The `StatementInterest` entries, empty when the bank returned none.
 */
@Serializable
data class StatementDetail(
    val accountId: String,
    val statementId: String,
    val reference: String,
    val type: String,
    val periodStart: String,
    val periodEnd: String,
    val created: String,
    val balances: List<StatementBalanceLine>,
    val fees: List<StatementCharge>,
    val interest: List<StatementCharge>,
)

/**
 * One `StatementAmount` entry — a named balance figure with its credit/debit sense.
 *
 * @property isCredit True when the OBIE `CreditDebitIndicator` is `Credit`, driving the display colour
 *   and sign.
 */
@Serializable
data class StatementBalanceLine(
    val type: String,
    val amount: String,
    val currency: String,
    val isCredit: Boolean,
)

/**
 * One `StatementFee` or `StatementInterest` entry — a described charge with its credit/debit sense.
 *
 * @property isCredit True when the OBIE `CreditDebitIndicator` is `Credit`; interest colours by it,
 *   fees render neutral regardless.
 */
@Serializable
data class StatementCharge(
    val description: String,
    val amount: String,
    val currency: String,
    val isCredit: Boolean,
)
