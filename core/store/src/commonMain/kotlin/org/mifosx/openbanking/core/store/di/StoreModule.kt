/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.di

import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mifosx.openbanking.core.store.AppStoreRegistry
import org.mifosx.openbanking.core.store.accounts.provideAccountsStore
import org.mifosx.openbanking.core.store.infra.StoreCacheManager
import org.mifosx.openbanking.core.store.infra.impl.StoreCacheManagerImpl
import org.mifosx.openbanking.core.store.transactions.provideTransactionsStore

/**
 * Koin module for app-level Store wiring.
 *
 * OBP banking `Store` instances are registered here in Phase 3, qualifier-bound via
 * [org.mifosx.openbanking.core.store.AppStoreRegistry], and each registered with the
 * [StoreCacheManager] for logout cache clearing.
 *
 * Wire into the Koin start-up:
 * ```kotlin
 * startKoin {
 *     modules(appStoreModule, /* ...other modules */)
 * }
 * ```
 */
val appStoreModule: Module = module {
    // Store cache manager — clears all registered caches on logout (registration-based)
    single<StoreCacheManager> {
        StoreCacheManagerImpl(
            bookkeeperDao = get(),
            draftDao = get(),
        )
    }

    // OBP banking stores. Accounts lands the canonical Store5 pattern; further
    // services adopt it in the data-layer fan-out.
    // Shared JSON for the offline cache payloads (lenient — tolerates OBP field drift).
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }

    single(AppStoreRegistry.Accounts) {
        provideAccountsStore(api = get(), config = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.Transactions) {
        provideTransactionsStore(api = get(), config = get(), dao = get(), json = get())
    }

    // Register stores with the cache manager so they clear on logout.
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.Accounts))
        mgr.register(get(AppStoreRegistry.Transactions))
    }
}
