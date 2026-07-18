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

import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mifosx.openbanking.core.store.AppStoreRegistry
import org.mifosx.openbanking.core.store.accounts.provideAccountsStore
import org.mifosx.openbanking.core.store.applications.provideAccountApplicationsStore
import org.mifosx.openbanking.core.store.atm.provideAtmStore
import org.mifosx.openbanking.core.store.cards.provideCardsStore
import org.mifosx.openbanking.core.store.cards.provideUserCardsStore
import org.mifosx.openbanking.core.store.customers.provideCustomersStore
import org.mifosx.openbanking.core.store.infra.StoreCacheManager
import org.mifosx.openbanking.core.store.infra.impl.StoreCacheManagerImpl
import org.mifosx.openbanking.core.store.kyc.provideKycDocumentsStore
import org.mifosx.openbanking.core.store.messages.provideCustomerMessagesStore
import org.mifosx.openbanking.core.store.payments.provideCounterpartiesStore
import org.mifosx.openbanking.core.store.products.provideProductsStore
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

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }

    single(AppStoreRegistry.Accounts) {
        provideAccountsStore(api = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.Transactions) {
        provideTransactionsStore(api = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.Cards) {
        provideCardsStore(api = get(), config = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.UserCards) {
        provideUserCardsStore(api = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.Counterparties) {
        provideCounterpartiesStore(api = get(), config = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.Customers) {
        provideCustomersStore(api = get(), config = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.AccountApplications) {
        provideAccountApplicationsStore(api = get(), config = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.CustomerMessages) {
        provideCustomerMessagesStore(api = get(), config = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.KycDocuments) {
        provideKycDocumentsStore(api = get(), config = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.Products) {
        provideProductsStore(api = get(), config = get(), dao = get(), json = get())
    }
    single(AppStoreRegistry.Atms) {
        provideAtmStore(api = get(), config = get(), dao = get(), json = get())
    }

    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.Accounts))
        mgr.register(get(AppStoreRegistry.Transactions))
        mgr.register(get(AppStoreRegistry.Cards))
        mgr.register(get(AppStoreRegistry.UserCards))
        mgr.register(get(AppStoreRegistry.Counterparties))
        mgr.register(get(AppStoreRegistry.Customers))
        mgr.register(get(AppStoreRegistry.AccountApplications))
        mgr.register(get(AppStoreRegistry.CustomerMessages))
        mgr.register(get(AppStoreRegistry.KycDocuments))
        mgr.register(get(AppStoreRegistry.Products))
        mgr.register(get(AppStoreRegistry.Atms))
    }
}
