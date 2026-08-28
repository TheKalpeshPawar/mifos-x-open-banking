/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.mifosx.openbanking.core.data.banking.AccountsOverviewRepository
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import template.core.base.common.screen.ScreenState
import kotlin.time.Clock
import kotlin.time.Instant

class FakeAccountsOverviewRepository : AccountsOverviewRepository {
    val emissions = MutableStateFlow<ScreenState<List<AccountWithBalance>>>(ScreenState.Loading)
    var refreshCount = 0
        private set

    override fun overviewState(scope: CoroutineScope): Flow<ScreenState<List<AccountWithBalance>>> = emissions

    override fun refresh() {
        refreshCount++
    }
}

/** A clock fixed at [instant], so the greeting band is assertable rather than wall-clock. */
class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}
