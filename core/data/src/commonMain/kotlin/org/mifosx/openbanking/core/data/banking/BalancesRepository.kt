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
import kotlinx.coroutines.flow.Flow
import org.mifosx.openbanking.core.model.banking.AccountBalance
import template.core.base.store.screen.ScreenDataStream

/**
 * Opens the balance stream for whichever account [accountIdFlow] currently selects, re-keying when
 * the selection changes. Balances are volatile, so the backing store is in-memory only.
 *
 * The id is a flow here because home switches account from its chip row without navigating, so the
 * key changes while the screen is alive.
 *
 * Each call builds a stream bound to the caller's [CoroutineScope], which the caller owns for the
 * lifetime of its screen.
 */
interface BalancesRepository {

    fun balanceStream(
        accountIdFlow: Flow<String>,
        scope: CoroutineScope,
    ): ScreenDataStream<AccountBalance>
}
