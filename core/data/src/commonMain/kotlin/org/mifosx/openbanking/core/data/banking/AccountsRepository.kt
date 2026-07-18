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
import org.mifosx.openbanking.core.model.banking.BankAccount
import template.core.base.common.screen.ScreenState

/**
 * Exposes the consented accounts as an offline-first [ScreenState] stream. Wraps the accounts Store
 * so the home ViewModel never sees Store5 directly — keeping it fakeable in tests.
 */
interface AccountsRepository {

    /** Streams the account list, decided into [ScreenState] against live connectivity. */
    fun accountsState(scope: CoroutineScope): Flow<ScreenState<List<BankAccount>>>

    /** Triggers a network refresh, preserving cached content while loading. */
    fun refresh()
}
