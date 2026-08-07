/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking

import kotlinx.coroutines.flow.Flow
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentHistoryItem
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt

/**
 * Local snapshot store for payment activity shown on the hub screen.
 *
 * Both successfully-submitted payments and pre-submission failures are persisted; only in-flight
 * submitted payments are ever refreshed from the API. Entries are limited to the 5 most recent.
 */
interface PaymentHistoryRepository {

    /** Emits the latest 5 rows, ordered by creation time descending. */
    fun observeRecent(): Flow<List<PaymentHistoryItem>>

    /** Persists a payment that the bank has accepted (any OBIE status, not just success). */
    suspend fun saveSubmitted(receipt: PaymentReceipt, draft: PaymentDraft)

    /** Persists a payment that failed before reaching submission. */
    suspend fun saveFailed(draft: PaymentDraft, errorKind: String, errorDescription: String)

    /**
     * For every stored payment whose status is still InProgress, fetches the current status from
     * the bank and updates the row. Terminal successes, terminal failures, and pre-submission
     * failures are skipped.
     */
    suspend fun refreshStatuses()
}
