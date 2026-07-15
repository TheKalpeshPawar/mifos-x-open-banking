/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.di

import com.russhwolf.settings.Settings
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mifosx.openbanking.core.network.HSBCUKSandboxConfig
import org.mifosx.openbanking.core.network.TOKEN_ENDPOINT
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.OAuth
import org.mifosx.openbanking.core.network.config.HsbcConfig
import org.mifosx.openbanking.core.network.getBaseUrl
import org.mifosx.openbanking.core.network.hsbcSandboxHttpClient

/**
 * Wires the HSBC sandbox networking graph. The mTLS [org.mifosx.openbanking.core.network.mtls.MtlsIdentity]
 * and the `named("hsbcSigningKey")` signing key come from [networkPlatformModule] (loaded synchronously
 * from platform resources), so the whole graph is plain synchronous singles — no suspend, no
 * `runBlocking`, no `Deferred`.
 */
val NetworkModule = module {
    includes(networkPlatformModule)

    single {
        hsbcSandboxHttpClient(
            settings = get<Settings>(named("secure")),
            identity = get(),
            signingKeyPem = get(named("hsbcSigningKey")),
        )
    }

    single {
        OAuth(
            httpClient = get(),
            tokenUrl = getBaseUrl(HSBCUKSandboxConfig.UKPersonal) + TOKEN_ENDPOINT,
            clientId = HsbcConfig.CLIENT_ID,
            kid = HsbcConfig.KID,
            signingKeyPem = get(named("hsbcSigningKey")),
        )
    }

    single { Aisp(get()) }
}
