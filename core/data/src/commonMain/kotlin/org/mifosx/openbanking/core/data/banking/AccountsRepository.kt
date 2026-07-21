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
import org.mifosx.openbanking.core.model.banking.BankAccount
import template.core.base.store.screen.ScreenDataStream

/**
 * Opens the stream of consented accounts. Wraps the accounts Store so callers never see Store5
 * directly, keeping it fakeable in tests.
 *
 * The whole consented set lives under one key, so no account id is needed. Each call builds a
 * stream bound to the caller's [CoroutineScope], which the caller owns for the lifetime of its
 * screen.
 */
interface AccountsRepository {

    fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<BankAccount>>
}
