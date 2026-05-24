/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.di

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mifosx.openbanking.core.data.alerts.AlertsRepository
import org.mifosx.openbanking.core.data.alerts.impl.AlertsRepositoryImpl
import org.mifosx.openbanking.core.data.crypto.CryptoRepository
import org.mifosx.openbanking.core.data.crypto.impl.CryptoRepositoryImpl
import org.mifosx.openbanking.core.data.currency.CurrencyRepository
import org.mifosx.openbanking.core.data.currency.impl.CurrencyRepositoryImpl
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.infra.impl.RoomFetchedAtRepository
import org.mifosx.openbanking.core.data.infra.impl.RoomSubmitOutbox
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.data.user.UserLogoutManager
import org.mifosx.openbanking.core.data.user.impl.UserDataRepositoryImpl
import org.mifosx.openbanking.core.data.user.impl.UserLogoutManagerImpl
import org.mifosx.openbanking.core.data.watchlist.WatchlistRepository
import org.mifosx.openbanking.core.data.watchlist.impl.WatchlistRepositoryImpl
import org.mifosx.openbanking.core.database.AppDatabase
import org.mifosx.openbanking.core.database.di.DatabaseModule
import org.mifosx.openbanking.core.datastore.di.DatastoreModule
import org.mifosx.openbanking.core.model.alerts.PriceAlert
import org.mifosx.openbanking.core.network.alerts.api.AlertsApi
import org.mifosx.openbanking.core.network.alerts.api.FakeAlertsApi
import org.mifosx.openbanking.core.network.di.NetworkModule
import org.mifosx.openbanking.core.store.AppStoreRegistry
import template.core.base.common.di.CommonModule
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.submit.OfflineSubmitSyncer
import template.core.base.store.submit.SubmitOutbox

val DataModule = module {
    includes(platformModule, CommonModule, DatabaseModule, DatastoreModule, NetworkModule)

    single<NetworkMonitor> { NetworkMonitorProvider.install() }
    singleOf(::UserDataRepositoryImpl) bind UserDataRepository::class

    // Framework FetchedAtRepository — durable lastFetchedAt persistence backing
    // DataFreshnessIndicator timestamps. Room-only by design (no in-memory fallback).
    single<FetchedAtRepository> { RoomFetchedAtRepository(get<AppDatabase>().fetchedAtDao) }

    // Framework DraftDao — backing store for SubmitOutbox / DraftSubmitHandler
    single { get<AppDatabase>().draftDao }

    // Personal watchlist — local-only persistence for the SubmitHandler showcase.
    single { get<AppDatabase>().watchlistDao }
    single<WatchlistRepository> { WatchlistRepositoryImpl(get()) }

    single<UserLogoutManager> { UserLogoutManagerImpl(get(), get(), get()) }

    // Fintech Repositories
    single<CurrencyRepository> {
        CurrencyRepositoryImpl(
            exchangeRatesStore = get(AppStoreRegistry.ExchangeRates),
            rateHistoryStore = get(AppStoreRegistry.RateHistory),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<CryptoRepository> {
        CryptoRepositoryImpl(
            coinMarketsStore = get(AppStoreRegistry.CoinMarkets),
            coinDetailStore = get(AppStoreRegistry.CoinDetail),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // Price alerts — fake-API-backed, with DraftSubmitHandler offline-resilience showcase.
    // Real forks substitute FakeAlertsApi with a Ktorfit-backed AlertsApi client.
    single<AlertsApi> { FakeAlertsApi() }
    single<AlertsRepository> { AlertsRepositoryImpl(api = get()) }

    // Outbox for PriceAlert payloads — RoomSubmitOutbox writes to framework_submit_drafts.
    single<SubmitOutbox<PriceAlert>> {
        RoomSubmitOutbox(dao = get(), serializer = PriceAlert.serializer())
    }

    // App-scoped CoroutineScope for cross-VM long-running coroutines (e.g., OfflineSubmitSyncer).
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Eager singleton — starts watching network online events at Koin start; retries
    // any pending alerts when connectivity returns.
    single(createdAtStart = true) {
        val syncer = OfflineSubmitSyncer<PriceAlert, PriceAlert>(
            scope = get(),
            outbox = get(),
            isOnlineFlow = get<NetworkMonitor>().isOnline,
            submitBlock = { payload -> get<AlertsApi>().create(payload) },
        )
        syncer.start()
        syncer
    }
}

expect val platformModule: Module
