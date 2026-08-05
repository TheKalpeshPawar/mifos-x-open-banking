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

import kotlinx.serialization.Serializable

/**
 * A submitted payment as the bank now reports it.
 *
 * @property domesticPaymentId The bank's id for the payment, and the key its status is read back
 *   under. Empty string when the payload omitted it.
 * @property consentId The consent this payment was executed against.
 * @property amountLabel The instructed amount echoed by the bank, formatted for display.
 * @property creditorName Who was paid, echoed from the submitted `Initiation`.
 * @property reference The remittance reference, or empty when the payment carried none.
 * @property debtorIdentification The paying account as the bank echoes it, unformatted.
 */
@Serializable
data class PaymentReceipt(
    val domesticPaymentId: String,
    val consentId: String,
    val status: PaymentStatus,
    val statusUpdateDateTime: String,
    val amountLabel: String,
    val creditorName: String,
    val reference: String = "",
    val debtorIdentification: String = "",
)
