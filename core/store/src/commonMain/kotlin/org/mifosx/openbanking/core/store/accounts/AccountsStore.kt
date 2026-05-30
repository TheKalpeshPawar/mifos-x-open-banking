/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.accounts

import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.AccountsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult
import template.core.base.store.infra.StoreFactory

/**
 * Store5 store for the authenticated user's accounts. Memory-backed (single-flight
 * dedup + in-memory cache + a streaming read for the UI); a Room source-of-truth for
 * full offline support is a per-screen follow-up. Keyed by [Unit] — one account list
 * per session. Errors are thrown so Store5 surfaces them as an error read-response.
 */
fun provideAccountsStore(api: AccountsApi, config: ObpConfig): Store<Unit, List<Account>> =
    StoreFactory.createMemoryStore(
        fetcher = Fetcher.of { _: Unit ->
            when (val result = api.listAccounts(config.bankId)) {
                is NetworkResult.Success -> result.data.accounts
                is NetworkResult.Error -> throw ObpException(reason = result.error.name)
            }
        },
    )
