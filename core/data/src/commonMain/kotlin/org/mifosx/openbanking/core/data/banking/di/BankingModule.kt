/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.mifosx.openbanking.core.data.banking.AccountsRepository
import org.mifosx.openbanking.core.data.banking.BalancesRepository
import org.mifosx.openbanking.core.data.banking.TransactionsRepository
import org.mifosx.openbanking.core.data.banking.impl.AccountsRepositoryImpl
import org.mifosx.openbanking.core.data.banking.impl.BalancesRepositoryImpl
import org.mifosx.openbanking.core.data.banking.impl.TransactionsRepositoryImpl
import org.mifosx.openbanking.core.data.banking.store.BankingStores
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.store.AppStoreRegistry
import org.mifosx.openbanking.core.store.infra.StoreCacheManager
import org.mifosx.openbanking.core.store.infra.impl.StoreCacheManagerImpl
import org.mobilenativefoundation.store.store5.Store

/**
 * Wires the home dashboard's Store5 stores and repositories.
 *
 * Each store is built with [BankingStores] (via the framework [template.core.base.store.infra.StoreFactory]),
 * bound under its [AppStoreRegistry] qualifier, and registered with the [StoreCacheManager] so it is
 * cleared on logout. The repositories wrap those stores; only the repository interfaces are exposed
 * to feature modules.
 */
val BankingModule: Module = module {

    single<Store<String, List<BankAccount>>>(AppStoreRegistry.Accounts) {
        BankingStores.accountsStore(aisp = get(), accountDao = get()).registerForLogout(get())
    }

    single<Store<String, AccountBalance>>(AppStoreRegistry.Balances) {
        BankingStores.balancesStore(aisp = get()).registerForLogout(get())
    }

    single<Store<String, List<TransactionItem>>>(AppStoreRegistry.Transactions) {
        BankingStores.transactionsStore(aisp = get(), transactionDao = get()).registerForLogout(get())
    }

    single<AccountsRepository> {
        AccountsRepositoryImpl(
            store = get(AppStoreRegistry.Accounts),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    single<BalancesRepository> {
        BalancesRepositoryImpl(
            store = get(AppStoreRegistry.Balances),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    single<TransactionsRepository> {
        TransactionsRepositoryImpl(
            store = get(AppStoreRegistry.Transactions),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
}

/**
 * Registers a store with the [StoreCacheManager] so logout clears its cache, returning the store for
 * fluent use in a `single { … }` binding. `register` lives only on [StoreCacheManagerImpl] (not the
 * interface) per the framework's documented usage; the safe cast is a no-op if the binding is faked.
 */
private fun <K : Any, V : Any> Store<K, V>.registerForLogout(cacheManager: StoreCacheManager): Store<K, V> {
    (cacheManager as? StoreCacheManagerImpl)?.register(this)
    return this
}
