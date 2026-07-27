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
import org.mifosx.openbanking.core.model.banking.StatementDetail
import org.mifosx.openbanking.core.model.banking.TransactionItem
import template.core.base.store.screen.ScreenDataStream

/**
 * Opens the two streams the statement-detail screen renders: the statement itself and the transactions
 * booked within its period. They come from distinct OBIE endpoints and cache independently, so the
 * screen merges them; the view model owns both streams for its lifetime.
 *
 * Both keys are plain values — they arrive as navigation arguments and are fixed for the screen's life.
 */
interface StatementDetailRepository {

    fun statementStream(
        accountId: String,
        statementId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<StatementDetail>

    fun statementTransactionsStream(
        accountId: String,
        statementId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<TransactionItem>>
}
