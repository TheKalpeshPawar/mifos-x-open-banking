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
    @SerialName("transaction_id") val transactionId: String = "",
    @SerialName("this_account") val thisAccount: TransactionAccount = TransactionAccount(),
    @SerialName("other_account") val otherAccount: TransactionCounterparty = TransactionCounterparty(),
    val details: TransactionDetails = TransactionDetails(),
    val metadata: TransactionMetadata = TransactionMetadata(),
    @SerialName("transaction_attributes") val transactionAttributes: List<TransactionAttribute> = emptyList(),
) {
    /** Stable id across v3.0.0 (`id`) and v6.0.0 (`transaction_id`) responses. */
    val txId: String get() = id.ifBlank { transactionId }

    /** Card that made this transaction — the CARD_ID attribute (= the card's bank_card_number). */
    val cardId: String? get() = attribute("CARD_ID")

    /** Standard transaction-type short code — the TXN_TYPE attribute (POS/ECOM/ATM/...). */
    val txnTypeCode: String? get() = attribute("TXN_TYPE")

    private fun attribute(name: String): String? =
        transactionAttributes.firstOrNull { it.name == name }?.value?.takeIf { it.isNotBlank() }
}

/** An OBP transaction attribute (name/type/value), returned inline by the v6 transactions endpoint. */
@Serializable
data class TransactionAttribute(
    val name: String = "",
    val type: String = "",
    val value: String = "",
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
