/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network.di

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import org.koin.dsl.module
import org.mifosx.openbanking.core.network.api.AccountsApi
import org.mifosx.openbanking.core.network.api.AuthApi
import org.mifosx.openbanking.core.network.api.createAccountsApi
import org.mifosx.openbanking.core.network.api.createAuthApi
import org.mifosx.openbanking.core.network.obp.InMemoryObpTokenProvider
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.network.obp.ObpTokenProvider
import org.mifosx.openbanking.core.network.obp.obpHttpClient
import org.mifosx.openbanking.core.network.obp.obpKtorfit

/**
 * OBP network graph: connection config, session-token holder, Ktor client (with the
 * DirectLogin auth plugin), the shared Ktorfit instance, and one Ktorfit service per
 * OBP endpoint group. New OBP services are registered here as their APIs are added.
 */
val NetworkModule = module {
    single { ObpConfig() }
    single<ObpTokenProvider> { InMemoryObpTokenProvider() }
    single<HttpClient> { obpHttpClient(config = get(), tokenProvider = get()) }
    single<Ktorfit> { obpKtorfit(client = get()) }

    single<AuthApi> { get<Ktorfit>().createAuthApi() }
    single<AccountsApi> { get<Ktorfit>().createAccountsApi() }
}
