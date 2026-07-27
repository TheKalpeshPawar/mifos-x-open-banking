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
import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import template.core.base.store.screen.ScreenDataStream

/**
 * Opens the two streams behind the account-detail screen: account metadata and the typed balance
 * rows, each from its own OBIE endpoint and its own store.
 *
 * The two streams are returned separately; the caller merges them with `combineScreenStates` and
 * refreshes both together.
 *
 * Each call builds a stream bound to the caller's [CoroutineScope], which the caller owns for the
 * lifetime of its screen.
 */
interface AccountDetailRepository {

    /** Account metadata for the header card. */
    fun detailStream(accountId: String, scope: CoroutineScope): ScreenDataStream<AccountDetail>

    /** Every typed balance row for the account, in the order the bank returned them. */
    fun balanceLinesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<AccountBalanceLine>>
}
