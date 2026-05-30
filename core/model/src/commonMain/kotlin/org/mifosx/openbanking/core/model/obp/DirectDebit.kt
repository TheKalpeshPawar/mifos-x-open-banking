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

/** OBP direct debit mandate. */
@Serializable
data class DirectDebit(
    @SerialName("direct_debit_id") val id: String = "",
    @SerialName("bank_id") val bankId: String = "",
    @SerialName("account_id") val accountId: String = "",
    @SerialName("counterparty_name") val counterpartyName: String = "",
    val amount: AmountOfMoney = AmountOfMoney(),
    @SerialName("date_signed") val dateSigned: String = "",
    @SerialName("date_cancelled") val dateCancelled: String = "",
    val active: Boolean = false,
)

@Serializable
data class DirectDebitsResponse(
    @SerialName("direct_debits") val directDebits: List<DirectDebit> = emptyList(),
)
