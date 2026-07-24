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

import org.mifosx.openbanking.core.network.model.ais.transactions.Transaction

/**
 * The HSBC sandbox stubs `MerchantDetails.MerchantName` with this exact constant on every booked
 * transaction. Because it is non-blank it would otherwise win over every real field and collapse the
 * whole list to one title, so it is treated as absent.
 */
internal const val SANDBOX_PLACEHOLDER_MERCHANT = "HSBC Sample merchant"

/**
 * The OBIE `MerchantDetails.MerchantName`, or null when it is blank or the sandbox placeholder — so a
 * caller falls back to a real per-transaction field instead of a uniform stub.
 */
internal fun Transaction.merchantNameOrNull(): String? =
    merchantDetails?.merchantName
        ?.takeIf { it.isNotBlank() && !it.equals(SANDBOX_PLACEHOLDER_MERCHANT, ignoreCase = true) }

/**
 * The best available human title for a transaction, most specific first: the real merchant name, then
 * the OBIE narrative (`TransactionInformation`), then the counterparty account name (creditor for an
 * outgoing payment, debtor for an incoming one). Null only when the payload carries none of them.
 */
internal fun Transaction.resolveTransactionTitle(): String? =
    merchantNameOrNull()
        ?: transactionInformation?.takeIf { it.isNotBlank() }
        ?: creditorAccount?.name?.takeIf { it.isNotBlank() }
        ?: debtorAccount?.name?.takeIf { it.isNotBlank() }
