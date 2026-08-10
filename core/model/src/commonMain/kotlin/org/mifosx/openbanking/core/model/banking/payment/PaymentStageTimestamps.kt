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
 * When a payment passed through the two stages the bank never reports.
 *
 * OBIE answers a status read with a single `CreationDateTime` and no per-stage history, so approval
 * and submission are only knowable from what this app observed as they happened and wrote to the
 * local payment-history row. They are deliberately not part of [PaymentReceipt], which is defined as
 * what the bank says: a receipt carrying locally-recorded fields would claim the bank told us
 * something it did not, and every wire mapper would have to fabricate a null to build one.
 *
 * Either value is null when nothing was recorded — a payment made on another device, or one whose
 * row has since been evicted by the five-row cap. A null stage renders undated rather than guessed.
 */
data class PaymentStageTimestamps(
    val approvedAt: String? = null,
    val submittedAt: String? = null,
)
