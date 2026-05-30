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
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.infra.impl.RoomFetchedAtRepository
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.data.user.UserLogoutManager
import org.mifosx.openbanking.core.data.user.impl.UserDataRepositoryImpl
import org.mifosx.openbanking.core.data.user.impl.UserLogoutManagerImpl
import org.mifosx.openbanking.core.database.AppDatabase
import org.mifosx.openbanking.core.database.di.DatabaseModule
import org.mifosx.openbanking.core.datastore.di.DatastoreModule
import org.mifosx.openbanking.core.network.di.NetworkModule
import template.core.base.common.di.CommonModule
import template.core.base.store.infra.FetchedAtRepository

val DataModule = module {
    includes(platformModule, CommonModule, DatabaseModule, DatastoreModule, NetworkModule)

    single<NetworkMonitor> { NetworkMonitorProvider.install() }
    singleOf(::UserDataRepositoryImpl) bind UserDataRepository::class

    // Framework FetchedAtRepository — durable lastFetchedAt persistence backing
    // DataFreshnessIndicator timestamps. Room-only by design (no in-memory fallback).
    single<FetchedAtRepository> { RoomFetchedAtRepository(get<AppDatabase>().fetchedAtDao) }

    // Framework DraftDao — generic backing store for SubmitOutbox / DraftSubmitHandler.
    // Reused by OBP banking submit flows (payments, standing orders) in Phase 3+.
    single { get<AppDatabase>().draftDao }

    single<UserLogoutManager> { UserLogoutManagerImpl(get(), get(), get()) }

    // App-scoped CoroutineScope for cross-VM long-running coroutines.
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // OBP banking repositories (Accounts, Transactions, Payments, …) are registered
    // here in Phase 3 — one Store5-backed repository per OBP service.
}

expect val platformModule: Module
