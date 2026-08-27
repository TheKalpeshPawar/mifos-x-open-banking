/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking.payment

/**
 * The two parties to a payment as the local row holds them.
 *
 * A payer the PSU chose at the bank is named on the submission response and on no later read, so a
 * status screen built only from [PaymentReceipt] shows an unnamed payer from the second view onwards.
 * These are the columns reconciled from every read, and they exist apart from [PaymentReceipt] for
 * the same reason [PaymentStageTimestamps] does: a receipt is defined as what the bank says on this
 * read, and these are what the app has recorded across all of them.
 *
 * Blank means nothing has ever been recorded for that field, matching the columns behind it.
 */
data class PaymentParties(
    val debtorName: String = "",
    val debtorIdentification: String = "",
    val debtorScheme: String = "",
    val creditorName: String = "",
    val creditorIdentification: String = "",
    val creditorScheme: String = "",
)
