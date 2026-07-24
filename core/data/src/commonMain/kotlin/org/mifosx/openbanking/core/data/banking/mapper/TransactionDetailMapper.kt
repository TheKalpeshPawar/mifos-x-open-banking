/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.model.banking.TransactionDetail
import org.mifosx.openbanking.core.network.model.ais.transactions.Transaction
import org.mifosx.openbanking.core.network.model.ais.transactions.TransactionsResponse

private const val CREDIT = "Credit"
private const val ZERO_AMOUNT = "0"

/**
 * Maps the OBIE `OBReadTransaction6` payload into the rich [TransactionDetail]s the detail screen
 * resolves against, for [accountId]. Every row keeps its full supplementary payload (merchant, status,
 * both dates, running balance, proprietary code) and gains a client-derived [TransactionDetail.category]
 * from the same MCC lookup the transactions list uses (see `deriveCategory`), so category derivation
 * stays in one place.
 *
 * The `MerchantCategoryCode` is deliberately blank-normalised to null so the caller's "hide the MCC row
 * when null" contract holds for both an absent field and an empty string.
 */
fun TransactionsResponse.toTransactionDetails(accountId: String): List<TransactionDetail> =
    data?.transaction.orEmpty().map { it.toTransactionDetail(accountId) }

private fun Transaction.toTransactionDetail(fallbackAccountId: String): TransactionDetail = TransactionDetail(
    transactionId = transactionId ?: "",
    accountId = accountId ?: fallbackAccountId,
    amount = amount?.amount ?: ZERO_AMOUNT,
    currency = amount?.currency ?: "",
    isCredit = creditDebitIndicator.equals(CREDIT, ignoreCase = true),
    merchantName = merchantDetails?.merchantName?.takeIf { it.isNotBlank() },
    status = status ?: "",
    bookingDateTime = bookingDateTime ?: "",
    valueDateTime = valueDateTime ?: "",
    category = deriveCategory(
        merchantCategoryCode = merchantDetails?.merchantCategoryCode,
        bankTransactionCode = bankTransactionCode?.code,
        proprietaryCode = proprietaryBankTransactionCode?.code,
    ),
    merchantCategoryCode = merchantDetails?.merchantCategoryCode?.takeIf { it.isNotBlank() },
    balanceAmount = balance?.amount?.amount ?: ZERO_AMOUNT,
    balanceCurrency = balance?.amount?.currency ?: "",
    balanceIsCredit = balance?.creditDebitIndicator.equals(CREDIT, ignoreCase = true),
    transactionInformation = transactionInformation ?: "",
    proprietaryCode = proprietaryBankTransactionCode?.code ?: "",
    proprietaryIssuer = proprietaryBankTransactionCode?.issuer ?: "",
)
