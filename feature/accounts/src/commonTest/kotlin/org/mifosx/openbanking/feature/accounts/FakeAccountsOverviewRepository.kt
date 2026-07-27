/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.mifosx.openbanking.core.data.banking.AccountsOverviewRepository
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import template.core.base.common.screen.ScreenState

class FakeAccountsOverviewRepository : AccountsOverviewRepository {
    val emissions = MutableStateFlow<ScreenState<List<AccountWithBalance>>>(ScreenState.Loading)
    var refreshCount = 0
        private set

    override fun overviewState(scope: CoroutineScope): Flow<ScreenState<List<AccountWithBalance>>> = emissions

    override fun refresh() {
        refreshCount++
    }
}
