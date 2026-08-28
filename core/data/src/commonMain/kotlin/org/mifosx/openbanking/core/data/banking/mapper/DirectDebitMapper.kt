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

import org.mifosx.openbanking.core.model.banking.DirectDebitItem
import org.mifosx.openbanking.core.model.banking.DirectDebitsSummary
import org.mifosx.openbanking.core.network.model.ais.directDebits.DirectDebit
import org.mifosx.openbanking.core.network.model.ais.directDebits.DirectDebitsResponse

private const val ACTIVE_STATUS = "ACTV"

/**
 * Maps the OBIE `OBReadDirectDebit2` payload into the mandate list, sorted active-first.
 *
 * The sort is stable, so within each status group the bank's own ordering survives — mandates do
 * not shuffle between refreshes for reasons the user cannot see.
 *
 * Mandates without a `Name` are dropped: the originator name is the only thing identifying a row to
 * a human, and a card headed by a blank line is worse than an absent one. Every other field
 * degrades to an empty string instead, so a mandate that has simply never been collected still
 * renders.
 */
fun DirectDebitsResponse.toDirectDebitItems(): List<DirectDebitItem> =
    data?.directDebit.orEmpty()
        .mapNotNull { it.toDirectDebitItemOrNull() }
        .sortedByDescending { it.isActive }

/**
 * Wraps [toDirectDebitItems] with the active/inactive tallies the summary chips render, so the
 * counts and the list are computed from one traversal of one payload and cannot drift apart.
 */
fun DirectDebitsResponse.toDirectDebitsSummary(): DirectDebitsSummary {
    val items = toDirectDebitItems()
    val activeCount = items.count { it.isActive }
    return DirectDebitsSummary(
        items = items,
        activeCount = activeCount,
        inactiveCount = items.size - activeCount,
    )
}

private fun DirectDebit.toDirectDebitItemOrNull(): DirectDebitItem? {
    val originator = name?.takeIf { it.isNotBlank() } ?: return null
    val status = directDebitStatusCode.orEmpty()
    return DirectDebitItem(
        mandateId = mandateRelatedInformation?.mandateIdentification.orEmpty(),
        name = originator,
        statusCode = status,
        isActive = status.equals(ACTIVE_STATUS, ignoreCase = true),
        previousPaymentAmount = previousPaymentAmount?.amount.orEmpty(),
        currency = previousPaymentAmount?.currency.orEmpty(),
        previousPaymentDateTime = previousPaymentDateTime.orEmpty(),
    )
}
