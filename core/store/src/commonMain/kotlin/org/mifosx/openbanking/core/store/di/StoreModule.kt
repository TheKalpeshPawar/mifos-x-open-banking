/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.store.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.mifosx.openbanking.core.store.infra.StoreCacheManager
import org.mifosx.openbanking.core.store.infra.impl.StoreCacheManagerImpl

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

    // OBP banking stores (Accounts, Transactions, …) are registered here in Phase 3
    // and registered with the StoreCacheManager for logout cache clearing.
}
