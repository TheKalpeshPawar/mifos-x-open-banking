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
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.accounts.impl.AccountsRepositoryImpl
import org.mifosx.openbanking.core.data.agents.AgentsRepository
import org.mifosx.openbanking.core.data.agents.AgentsRepositoryImpl
import org.mifosx.openbanking.core.data.applications.AccountApplicationsRepository
import org.mifosx.openbanking.core.data.applications.AccountApplicationsRepositoryImpl
import org.mifosx.openbanking.core.data.atm.AtmRepository
import org.mifosx.openbanking.core.data.atm.AtmRepositoryImpl
import org.mifosx.openbanking.core.data.auth.AuthRecoveryRepository
import org.mifosx.openbanking.core.data.auth.AuthRecoveryRepositoryImpl
import org.mifosx.openbanking.core.data.auth.ObpAuthRepository
import org.mifosx.openbanking.core.data.auth.impl.ObpAuthRepositoryImpl
import org.mifosx.openbanking.core.data.banks.BanksRepository
import org.mifosx.openbanking.core.data.banks.BanksRepositoryImpl
import org.mifosx.openbanking.core.data.cards.CardsRepository
import org.mifosx.openbanking.core.data.cards.CardsRepositoryImpl
import org.mifosx.openbanking.core.data.consents.ConsentsRepository
import org.mifosx.openbanking.core.data.consents.ConsentsRepositoryImpl
import org.mifosx.openbanking.core.data.customers.CustomersRepository
import org.mifosx.openbanking.core.data.customers.CustomersRepositoryImpl
import org.mifosx.openbanking.core.data.directdebits.DirectDebitsRepository
import org.mifosx.openbanking.core.data.directdebits.DirectDebitsRepositoryImpl
import org.mifosx.openbanking.core.data.fx.FxRepository
import org.mifosx.openbanking.core.data.fx.FxRepositoryImpl
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.infra.impl.RoomFetchedAtRepository
import org.mifosx.openbanking.core.data.kyc.KycRepository
import org.mifosx.openbanking.core.data.kyc.KycRepositoryImpl
import org.mifosx.openbanking.core.data.meetings.MeetingsRepository
import org.mifosx.openbanking.core.data.meetings.MeetingsRepositoryImpl
import org.mifosx.openbanking.core.data.messages.CustomerMessagesRepository
import org.mifosx.openbanking.core.data.messages.CustomerMessagesRepositoryImpl
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepositoryImpl
import org.mifosx.openbanking.core.data.pfm.PfmRepository
import org.mifosx.openbanking.core.data.pfm.PfmRepositoryImpl
import org.mifosx.openbanking.core.data.products.ProductsRepository
import org.mifosx.openbanking.core.data.products.ProductsRepositoryImpl
import org.mifosx.openbanking.core.data.profile.ProfileRepository
import org.mifosx.openbanking.core.data.profile.ProfileRepositoryImpl
import org.mifosx.openbanking.core.data.standingorders.StandingOrdersRepository
import org.mifosx.openbanking.core.data.standingorders.StandingOrdersRepositoryImpl
import org.mifosx.openbanking.core.data.transactions.TransactionMetadataRepository
import org.mifosx.openbanking.core.data.transactions.TransactionMetadataRepositoryImpl
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.data.transactions.TransactionsRepositoryImpl
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.data.user.UserLogoutManager
import org.mifosx.openbanking.core.data.user.impl.UserDataRepositoryImpl
import org.mifosx.openbanking.core.data.user.impl.UserLogoutManagerImpl
import org.mifosx.openbanking.core.database.AppDatabase
import org.mifosx.openbanking.core.database.di.DatabaseModule
import org.mifosx.openbanking.core.datastore.di.DatastoreModule
import org.mifosx.openbanking.core.network.di.NetworkModule
import org.mifosx.openbanking.core.store.AppStoreRegistry
import template.core.base.common.di.CommonModule
import template.core.base.store.infra.FetchedAtRepository

val DataModule = module {
    includes(platformModule, CommonModule, DatabaseModule, DatastoreModule, NetworkModule)

    single<NetworkMonitor> { NetworkMonitorProvider.install() }
    singleOf(::UserDataRepositoryImpl) bind UserDataRepository::class

    // Framework FetchedAtRepository — durable lastFetchedAt persistence backing
    // DataFreshnessIndicator timestamps. Room-only by design (no in-memory fallback).
    single<FetchedAtRepository> { RoomFetchedAtRepository(get<AppDatabase>().fetchedAtDao) }

    single { get<AppDatabase>().draftDao }

    single<UserLogoutManager> { UserLogoutManagerImpl(get(), get(), get()) }

    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<ObpAuthRepository> { ObpAuthRepositoryImpl(authApi = get(), config = get(), tokenProvider = get()) }
    single<AccountsRepository> {
        AccountsRepositoryImpl(
            api = get(),
            config = get(),
            accountsStore = get(AppStoreRegistry.Accounts),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<TransactionsRepository> {
        TransactionsRepositoryImpl(
            api = get(),
            transactionsStore = get(AppStoreRegistry.Transactions),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<TransactionMetadataRepository> { TransactionMetadataRepositoryImpl(api = get()) }
    single<CardsRepository> {
        CardsRepositoryImpl(
            api = get(),
            config = get(),
            cardsStore = get(AppStoreRegistry.Cards),
            userCardsStore = get(AppStoreRegistry.UserCards),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<PaymentsRepository> {
        PaymentsRepositoryImpl(
            api = get(),
            config = get(),
            counterpartiesStore = get(AppStoreRegistry.Counterparties),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<StandingOrdersRepository> {
        StandingOrdersRepositoryImpl(
            api = get(),
            transactionsRepository = get(),
            customersRepository = get(),
            profileRepository = get(),
            config = get(),
            dao = get(),
            json = get(),
        )
    }
    single<DirectDebitsRepository> {
        DirectDebitsRepositoryImpl(
            api = get(),
            config = get(),
            directDebitsStore = get(AppStoreRegistry.DirectDebits),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<BanksRepository> { BanksRepositoryImpl(api = get()) }
    single<FxRepository> { FxRepositoryImpl(api = get(), config = get()) }
    single<AtmRepository> {
        AtmRepositoryImpl(
            api = get(),
            config = get(),
            atmsStore = get(AppStoreRegistry.Atms),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<ProductsRepository> {
        ProductsRepositoryImpl(
            api = get(),
            config = get(),
            productsStore = get(AppStoreRegistry.Products),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<CustomersRepository> {
        CustomersRepositoryImpl(
            api = get(),
            config = get(),
            customersStore = get(AppStoreRegistry.Customers),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<KycRepository> {
        KycRepositoryImpl(
            api = get(),
            config = get(),
            documentsStore = get(AppStoreRegistry.KycDocuments),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<ConsentsRepository> { ConsentsRepositoryImpl(api = get()) }
    single<AccountApplicationsRepository> {
        AccountApplicationsRepositoryImpl(
            api = get(),
            config = get(),
            applicationsStore = get(AppStoreRegistry.AccountApplications),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<CustomerMessagesRepository> {
        CustomerMessagesRepositoryImpl(
            api = get(),
            config = get(),
            messagesStore = get(AppStoreRegistry.CustomerMessages),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<MeetingsRepository> { MeetingsRepositoryImpl(api = get(), config = get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(api = get()) }
    single<AgentsRepository> { AgentsRepositoryImpl(api = get(), config = get()) }
    single<PfmRepository> { PfmRepositoryImpl(api = get()) }
    single<AuthRecoveryRepository> { AuthRecoveryRepositoryImpl(api = get()) }
}

expect val platformModule: Module
