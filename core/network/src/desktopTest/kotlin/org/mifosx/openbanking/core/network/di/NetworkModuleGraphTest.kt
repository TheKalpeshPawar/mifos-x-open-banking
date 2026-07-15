/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.di

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.OAuth
import kotlin.test.Test
import kotlin.test.assertNotNull

class NetworkModuleGraphTest {

    /**
     * Resolves the whole [NetworkModule] graph on desktop: this instantiates the mTLS [HttpClient]
     * (built from the committed throwaway PKCS#12 fixture via the desktop cert loader), [OAuth] and
     * [Aisp]. It proves the DI wiring is complete and that the synchronous cert injection yields a
     * buildable client. The only external dependency — the `named("secure")` [Settings] — is supplied
     * here in-memory (normally from the datastore module).
     */
    @Test
    fun `NetworkModule resolves the mTLS client, OAuth and Aisp`() {
        val app = koinApplication {
            modules(
                module { single<Settings>(named("secure")) { Settings() } },
                NetworkModule,
            )
        }
        val koin = app.koin

        val client = koin.get<HttpClient>()
        assertNotNull(client)
        assertNotNull(koin.get<OAuth>())
        assertNotNull(koin.get<Aisp>())

        client.close()
        app.close()
    }
}
