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

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.model.banking.TransactionDetail
import template.core.base.store.screen.ScreenDataStream

/**
 * Opens the stream of rich transaction records for one account.
 *
 * OBIE has no single-transaction endpoint, so the stream carries the whole account list and the
 * detail view model resolves its record by filtering on `transactionId`. Each call builds a stream
 * bound to the caller's [CoroutineScope], which the caller owns for the lifetime of its screen. The
 * account id is a plain value: it arrives as a navigation argument and is fixed for that lifetime.
 *
 * The stream emits `Content` even when the account has no transactions; a caller resolving a single
 * record turns an absent match into an empty screen state.
 */
interface TransactionDetailRepository {

    fun transactionDetailStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<TransactionDetail>>
}
