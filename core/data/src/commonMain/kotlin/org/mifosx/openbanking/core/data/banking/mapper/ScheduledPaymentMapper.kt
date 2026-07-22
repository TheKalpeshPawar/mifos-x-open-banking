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

import org.mifosx.openbanking.core.model.banking.ScheduledPaymentItem
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentType
import org.mifosx.openbanking.core.network.model.ais.scheduledPayments.ScheduledPayment
import org.mifosx.openbanking.core.network.model.ais.scheduledPayments.ScheduledPaymentsResponse

private const val EXECUTION_TYPE = "execution"
private const val ARRIVAL_TYPE = "arrival"

/**
 * Maps the OBIE `OBReadScheduledPayment3` payload into the scheduled-payment list.
 *
 * The bank returns the payments in its own order, which is preserved: unlike direct debits there is
 * no active/inactive split to sort on, so re-ordering would only make the list shuffle between
 * refreshes for reasons the user cannot see.
 *
 * Nothing is dropped — every entry the bank sends renders. Absent fields degrade to an empty string
 * (or [ScheduledPaymentType.Unknown]) so a payment missing an optional detail still shows rather
 * than vanishing. [accountId] is the requested id, used only when the payload omits its own.
 */
fun ScheduledPaymentsResponse.toScheduledPaymentItems(accountId: String): List<ScheduledPaymentItem> =
    data?.scheduledPayment.orEmpty().map { it.toScheduledPaymentItem(accountId) }

private fun ScheduledPayment.toScheduledPaymentItem(requestedAccountId: String): ScheduledPaymentItem =
    ScheduledPaymentItem(
        scheduledPaymentId = scheduledPaymentId.orEmpty(),
        accountId = accountId ?: requestedAccountId,
        payeeName = creditorAccount?.name.orEmpty(),
        amount = instructedAmount?.amount.orEmpty(),
        currency = instructedAmount?.currency.orEmpty(),
        scheduledDateTime = scheduledPaymentDateTime.orEmpty(),
        scheduledType = scheduledType.toScheduledPaymentType(),
        reference = reference.orEmpty(),
        creditorIdentification = creditorAccount?.identification.orEmpty(),
    )

/** Parses the OBIE `ScheduledType` string, case-insensitively, into the domain enum. */
private fun String?.toScheduledPaymentType(): ScheduledPaymentType = when (this?.trim()?.lowercase()) {
    EXECUTION_TYPE -> ScheduledPaymentType.Execution
    ARRIVAL_TYPE -> ScheduledPaymentType.Arrival
    else -> ScheduledPaymentType.Unknown
}
