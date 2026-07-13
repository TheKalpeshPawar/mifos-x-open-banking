/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.ais.statementTransactions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    @SerialName("AccountId")
    val accountId: String? = null,
    @SerialName("TransactionId")
    val transactionId: String? = null,
    @SerialName("TransactionReference")
    val transactionReference: String? = null,
    @SerialName("StatementReference")
    val statementReference: List<String>? = null,
    @SerialName("CreditDebitIndicator")
    val creditDebitIndicator: String? = null,
    @SerialName("Status")
    val status: String? = null,
    @SerialName("TransactionMutability")
    val transactionMutability: String? = null,
    @SerialName("BookingDateTime")
    val bookingDateTime: String? = null,
    @SerialName("ValueDateTime")
    val valueDateTime: String? = null,
    @SerialName("TransactionInformation")
    val transactionInformation: String? = null,
    @SerialName("AddressLine")
    val addressLine: String? = null,
    @SerialName("Amount")
    val amount: Amount? = null,
    @SerialName("BankTransactionCode")
    val bankTransactionCode: BankTransactionCode? = null,
    @SerialName("ProprietaryBankTransactionCode")
    val proprietaryBankTransactionCode: ProprietaryBankTransactionCode? = null,
    @SerialName("Balance")
    val balance: Balance? = null,
    @SerialName("MerchantDetails")
    val merchantDetails: MerchantDetails? = null,
    @SerialName("CreditorAccount")
    val creditorAccount: CreditorAccount? = null,
    @SerialName("DebtorAccount")
    val debtorAccount: DebtorAccount? = null,
    @SerialName("CardInstrument")
    val cardInstrument: CardInstrument? = null,
)
