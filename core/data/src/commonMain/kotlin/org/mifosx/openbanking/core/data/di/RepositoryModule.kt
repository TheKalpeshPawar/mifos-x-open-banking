/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.di

import com.russhwolf.settings.Settings
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.data.banking.di.BankingModule
import org.mifosx.openbanking.core.data.banking.impl.PaymentInitiationRepositoryImpl
import org.mifosx.openbanking.core.data.callback.ConsentCallbackRepository
import org.mifosx.openbanking.core.data.callback.ConsentSession
import org.mifosx.openbanking.core.data.callback.PaymentAuthRepository
import org.mifosx.openbanking.core.data.callback.PaymentAuthSession
import org.mifosx.openbanking.core.data.callback.PendingAuthStore
import org.mifosx.openbanking.core.data.callback.SettingsConsentSession
import org.mifosx.openbanking.core.data.callback.SettingsPaymentAuthSession
import org.mifosx.openbanking.core.data.callback.SettingsPendingAuthStore
import org.mifosx.openbanking.core.data.callback.impl.ConsentCallbackRepositoryImpl
import org.mifosx.openbanking.core.data.callback.impl.PaymentAuthRepositoryImpl
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.infra.impl.RoomFetchedAtRepository
import org.mifosx.openbanking.core.data.login.LoginRepository
import org.mifosx.openbanking.core.data.login.impl.LoginRepositoryImpl
import org.mifosx.openbanking.core.data.user.AppLogout
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.data.user.UserLogoutManager
import org.mifosx.openbanking.core.data.user.impl.AppLogoutImpl
import org.mifosx.openbanking.core.data.user.impl.UserDataRepositoryImpl
import org.mifosx.openbanking.core.data.user.impl.UserLogoutManagerImpl
import org.mifosx.openbanking.core.database.AppDatabase
import org.mifosx.openbanking.core.database.di.DatabaseModule
import org.mifosx.openbanking.core.datastore.di.DatastoreModule
import org.mifosx.openbanking.core.network.di.NetworkModule
import template.core.base.common.di.CommonModule
import template.core.base.store.infra.FetchedAtRepository
import kotlin.time.Clock

val DataModule = module {
    includes(platformModule, CommonModule, DatabaseModule, DatastoreModule, NetworkModule, BankingModule)

    single<NetworkMonitor> { NetworkMonitorProvider.install() }
    singleOf(::UserDataRepositoryImpl) bind UserDataRepository::class

    single<LoginRepository> {
        LoginRepositoryImpl(
            oauth = get(),
            aisp = get(),
            signingKeyPem = get(named("hsbcSigningKey")),
            clientId = get(named("hsbcClientId")),
            kid = get(named("hsbcKid")),
            bankHost = get(named("hsbcBankHost")),
            authorizeHost = get(named("hsbcAuthorizeHost")),
            redirectUri = get(named("hsbcRedirectUri")),
        )
    }

    single<ConsentSession> { SettingsConsentSession(secureSettings = get<Settings>(named("secure"))) }

    single<PendingAuthStore> {
        SettingsPendingAuthStore(
            secureSettings = get<Settings>(named("secure")),
            nowEpochSeconds = { Clock.System.now().epochSeconds },
        )
    }

    single<ConsentCallbackRepository> {
        ConsentCallbackRepositoryImpl(
            oauth = get(),
            aisp = get(),
            pendingAuthStore = get(),
        )
    }

    single<PaymentAuthSession> { SettingsPaymentAuthSession(secureSettings = get<Settings>(named("secure"))) }

    single<PaymentAuthRepository> {
        PaymentAuthRepositoryImpl(
            oauth = get(),
            pisp = get(),
            paymentAuthSession = get(),
            redirectUri = get(named("hsbcRedirectUri")),
        )
    }

    single<PaymentInitiationRepository> {
        PaymentInitiationRepositoryImpl(
            pisp = get(),
            oauth = get(),
            paymentAuthSession = get(),
            capabilityRegistry = get(),
            signingKeyPem = get(named("hsbcSigningKey")),
            clientId = get(named("hsbcClientId")),
            kid = get(named("hsbcKid")),
            bankHost = get(named("hsbcBankHost")),
            authorizeHost = get(named("hsbcAuthorizeHost")),
            redirectUri = get(named("hsbcRedirectUri")),
        )
    }

    single<FetchedAtRepository> { RoomFetchedAtRepository(get<AppDatabase>().fetchedAtDao) }

    single { get<AppDatabase>().draftDao }

    single<UserLogoutManager> { UserLogoutManagerImpl(get(), get(), get()) }

    single<AppLogout> {
        AppLogoutImpl(
            consentRevokeRepository = get(),
            consentSession = get(),
            paymentAuthSession = get(),
            userDataRepository = get(),
            storeCacheManager = get(),
        )
    }

    // App-scoped  for cross-VM long-running coroutines.
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}

expect val platformModule: Module
