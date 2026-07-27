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
 * One statement period, derived from OBIE `GET /accounts/{AccountId}/statements`.
 *
 * Every field stays as the raw OBIE string exactly as the bank returned it. Formatting — turning the
 * ISO-8601 period into a readable range, or the decimal string into a currency figure — is a display
 * concern and belongs in the view model, where the currency symbol and thousands separators are
 * applied. Keeping the model raw means the same period renders identically regardless of who consumes
 * it and never loses precision to a premature parse.
 *
 * @property statementId OBIE `StatementId` — the reference the file download and detail calls are
 *   keyed by. A statement without one is dropped by the mapper, so this is never empty.
 * @property statementReference OBIE `StatementReference`, the human-facing label the bank prints on
 *   the statement. Empty string when the payload carried none.
 * @property startDateTime ISO-8601 `StartDateTime` marking the first day the period covers; formatted
 *   for display in the view model and used to sort periods newest-first.
 * @property endDateTime ISO-8601 `EndDateTime` marking the last day the period covers.
 * @property closingBalanceAmount Raw OBIE decimal string from the `ClosingBalance` `StatementAmount`.
 *   Empty string when the statement declared no closing balance.
 * @property closingBalanceCurrency ISO-4217 code paired with [closingBalanceAmount]. Empty string
 *   when the statement declared no closing balance.
 */
@Serializable
data class StatementPeriod(
    val statementId: String,
    val statementReference: String,
    val startDateTime: String,
    val endDateTime: String,
    val closingBalanceAmount: String,
    val closingBalanceCurrency: String,
)
