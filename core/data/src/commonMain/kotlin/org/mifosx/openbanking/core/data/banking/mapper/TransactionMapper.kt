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

import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.network.model.ais.transactions.Transaction
import org.mifosx.openbanking.core.network.model.ais.transactions.TransactionsResponse

private const val CREDIT = "Credit"
private const val ZERO_AMOUNT = "0"

/**
 * Maps the OBIE `OBReadTransaction6` payload into UI-facing [TransactionItem]s for [accountId].
 * Prefers the merchant name for the row title, falling back to the OBIE transaction narrative.
 */
fun TransactionsResponse.toTransactionItems(accountId: String): List<TransactionItem> =
    data?.transaction.orEmpty().map { it.toTransactionItem(accountId) }

private fun Transaction.toTransactionItem(fallbackAccountId: String): TransactionItem = TransactionItem(
    transactionId = transactionId ?: "",
    accountId = accountId ?: fallbackAccountId,
    description = merchantDetails?.merchantName?.takeIf { it.isNotBlank() }
        ?: transactionInformation
        ?: "",
    bookingDateTime = bookingDateTime ?: "",
    amount = amount?.amount ?: ZERO_AMOUNT,
    currency = amount?.currency ?: "",
    isCredit = creditDebitIndicator.equals(CREDIT, ignoreCase = true),
)
