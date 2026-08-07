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
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
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
     * Which rail a submitted payment was sent on, so its status is read from the right endpoint.
     *
     * The two rails have separate status endpoints, and an id is only valid against its own — an
     * international payment looked up as domestic answers 404. The rail is not derivable from the
     * id, so it is read back from the row written at submission.
     *
     * Returns null when nothing is stored for [paymentId], which the caller must decide about
     * rather than have guessed for it.
     */
    suspend fun railOf(paymentId: String): PaymentRail?

    /**
     * For every stored payment whose status is still InProgress, fetches the current status from
     * the bank and updates the row. Terminal successes, terminal failures, and pre-submission
     * failures are skipped.
     */
    suspend fun refreshStatuses()
}
