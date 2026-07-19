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

import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.core.model.banking.TransactionListItem
import org.mifosx.openbanking.core.model.banking.TransactionsPage
import org.mifosx.openbanking.core.network.model.ais.transactions.Transaction
import org.mifosx.openbanking.core.network.model.ais.transactions.TransactionsResponse

private const val CREDIT = "Credit"
private const val ZERO_AMOUNT = "0"

/**
 * Maps an OBIE `OBReadTransaction6` payload into a [TransactionsPage] for [accountId], carrying the
 * `Links.Next` cursor and `Meta.TotalPages` so the caller can page transparently. Each row gains a
 * client-derived [TransactionCategory] and pending flag (OBIE returns neither directly).
 */
fun TransactionsResponse.toTransactionsPage(accountId: String): TransactionsPage = TransactionsPage(
    items = data?.transaction.orEmpty().map { it.toTransactionListItem(accountId) },
    nextLink = links?.next?.takeIf { it.isNotBlank() },
    totalPages = meta?.totalPages,
)

private fun Transaction.toTransactionListItem(fallbackAccountId: String): TransactionListItem = TransactionListItem(
    transactionId = transactionId ?: "",
    accountId = accountId ?: fallbackAccountId,
    description = merchantDetails?.merchantName?.takeIf { it.isNotBlank() }
        ?: transactionInformation
        ?: "",
    bookingDateTime = bookingDateTime ?: "",
    amount = amount?.amount ?: ZERO_AMOUNT,
    currency = amount?.currency ?: "",
    isCredit = creditDebitIndicator.equals(CREDIT, ignoreCase = true),
    category = deriveCategory(
        merchantCategoryCode = merchantDetails?.merchantCategoryCode,
        proprietaryCode = proprietaryBankTransactionCode?.code,
    ),
    isPending = isPendingStatus(status),
)

/** OBIE booked status is `BOOK`; a pending entry reports `PDNG` (or the sandbox's `Pending`). */
internal fun isPendingStatus(status: String?): Boolean =
    status?.let { it.equals("PDNG", ignoreCase = true) || it.equals("Pending", ignoreCase = true) } ?: false

/**
 * Infers a personal-finance [TransactionCategory] client-side from the ISO-18245 merchant category
 * code, falling back to a transfer hint on the proprietary bank-transaction code, then
 * [TransactionCategory.OTHER]. The debit/credit direction is deliberately NOT used: a credit can be a
 * refund of a categorised purchase, so treating credits as income would be wrong.
 */
internal fun deriveCategory(
    merchantCategoryCode: String?,
    proprietaryCode: String?,
): TransactionCategory {
    val byMcc = merchantCategoryCode?.trim()?.toIntOrNull()?.let(::categoryForMcc)
    return byMcc ?: proprietaryCode.transferHintOrOther()
}

private fun String?.transferHintOrOther(): TransactionCategory {
    val proprietary = this?.uppercase()
    val isTransfer = proprietary != null && (proprietary.contains("TRF") || proprietary.contains("TRANSFER"))
    return if (isTransfer) TransactionCategory.TRANSFER else TransactionCategory.OTHER
}

private fun categoryForMcc(mcc: Int): TransactionCategory? = when (mcc) {
    in GROCERY_MCCS -> TransactionCategory.GROCERIES
    in DINING_MCCS -> TransactionCategory.DINING
    in SUBSCRIPTION_MCCS -> TransactionCategory.SUBSCRIPTIONS
    in TRANSPORT_MCCS -> TransactionCategory.TRANSPORT
    in SHOPPING_MCCS -> TransactionCategory.SHOPPING
    else -> null
}

// ISO-18245 merchant category codes grouped into personal-finance buckets.
private val GROCERY_MCCS = setOf(5411, 5422, 5441, 5451, 5462, 5499)
private val DINING_MCCS = setOf(5811, 5812, 5813, 5814)
private val SUBSCRIPTION_MCCS = setOf(4816, 4899, 5815, 5816, 5817, 5818, 5968)
private val TRANSPORT_MCCS = setOf(4111, 4121, 4131, 4784, 4789, 5541, 5542, 7523)
private val SHOPPING_MCCS = setOf(
    5200, 5211, 5300, 5310, 5311, 5331, 5399, 5611, 5621, 5651, 5655, 5661, 5691, 5699,
    5732, 5733, 5734, 5735, 5912, 5942, 5943, 5945, 5947, 5999,
)
