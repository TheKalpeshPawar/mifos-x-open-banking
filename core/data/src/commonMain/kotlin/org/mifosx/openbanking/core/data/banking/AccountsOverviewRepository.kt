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
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import template.core.base.common.screen.ScreenState

/**
 * Streams the accounts overview: every consented account paired with its balance.
 *
 * Fans out over the accounts stream, resolving each account's balance concurrently. A single failed
 * balance fetch leaves that account with a null balance rather than failing the whole screen.
 */
interface AccountsOverviewRepository {

    /**
     * The overview as a unified [ScreenState] stream. Non-[ScreenState.Content] states from the
     * underlying accounts stream pass through unchanged; an empty account set surfaces as
     * [ScreenState.Empty].
     */
    fun overviewState(scope: CoroutineScope): Flow<ScreenState<List<AccountWithBalance>>>

    /** Refreshes the accounts, then re-resolves each account's balance from the network. */
    fun refresh()
}
