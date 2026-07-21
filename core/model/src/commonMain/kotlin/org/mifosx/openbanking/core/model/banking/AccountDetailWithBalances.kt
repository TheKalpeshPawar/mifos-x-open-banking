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
 * The account-detail screen's complete payload — metadata plus every typed balance row.
 *
 * OBIE serves the two halves from separate endpoints, so they are fetched independently and merged.
 *
 * An empty [balances] is a content outcome rather than an empty screen state: the header and
 * navigation chips still render for an account the bank reports no balances for.
 *
 * @property detail Account metadata backing the header card.
 * @property balances Typed balance rows in the order the bank returned them; may be empty.
 */
@Serializable
data class AccountDetailWithBalances(
    val detail: AccountDetail,
    val balances: List<AccountBalanceLine>,
) {
    /** True when the bank returned no balance rows — drives the in-content empty block. */
    val hasNoBalances: Boolean get() = balances.isEmpty()
}
