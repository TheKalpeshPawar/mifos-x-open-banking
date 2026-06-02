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

/** OBP transaction with its source account, counterparty, financial details and metadata. */
@Serializable
data class Transaction(
    val id: String = "",
    @SerialName("this_account") val thisAccount: TransactionAccount = TransactionAccount(),
    @SerialName("other_account") val otherAccount: TransactionCounterparty = TransactionCounterparty(),
    val details: TransactionDetails = TransactionDetails(),
    val metadata: TransactionMetadata = TransactionMetadata(),
)

@Serializable
data class TransactionAccount(
    val id: String = "",
    @SerialName("bank_id") val bankId: String = "",
    val label: String = "",
)

@Serializable
data class TransactionCounterparty(
    val id: String = "",
    @SerialName("holder") val holder: CounterpartyHolder = CounterpartyHolder(),
)

@Serializable
data class CounterpartyHolder(
    val name: String = "",
    @SerialName("is_alias") val isAlias: Boolean = false,
)

@Serializable
data class TransactionDetails(
    val type: String = "",
    val description: String = "",
    val posted: String = "",
    val completed: String = "",
    @SerialName("new_balance") val newBalance: AmountOfMoney = AmountOfMoney(),
    val value: AmountOfMoney = AmountOfMoney(),
)

@Serializable
data class TransactionMetadata(
    val narrative: String = "",
)

/** Wrapper for the OBP transactions-list response (`{ "transactions": [...] }`). */
@Serializable
data class TransactionsResponse(
    val transactions: List<Transaction> = emptyList(),
)
