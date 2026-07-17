/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.mtls

import template.core.base.network.httpClient
import kotlin.test.Test
import kotlin.test.assertNotNull

class MtlsInstallTest {

    @Test
    fun `installMtls builds a client from a valid PKCS12 identity`() {
        // Exercises the whole JVM path: KeyStore load → KeyManagerFactory → TrustManagerFactory →
        // SSLContext → OkHttp sslSocketFactory. Building the client runs installMtls synchronously.
        val client = httpClient {
            installMtls(MtlsIdentity(generateTestPkcs12(), "testpass"))
        }
        assertNotNull(client)
        client.close()
    }

    @Test
    fun `installMtls on the JVM accepts an empty passphrase that Android rejects`() {
        // Pins the platform divergence rather than hiding it. A PKCS#12 exported with an empty
        // passphrase is still PBES2/AES-encrypted — the empty string is simply what PBKDF2 is keyed
        // on. The JVM derives that key without complaint (asserted here); Android's BouncyCastle
        // refuses a zero-length password outright. So THIS test passing says nothing about Android,
        // and no desktop test can: `KeyStore.getInstance("PKCS12")` resolves to SUN here and to BC
        // there. The Android contract is covered on-device by
        // cmp-android MtlsRealAssetTest.installMtlsLoadsTheShippedTransportIdentity.
        val client = httpClient {
            installMtls(MtlsIdentity(generateTestPkcs12(password = ""), pkcs12Password = ""))
        }
        assertNotNull(client)
        client.close()
    }

    @Test
    fun `installMtls fails on a corrupt PKCS12 identity`() {
        val error = runCatching {
            httpClient {
                installMtls(MtlsIdentity(byteArrayOf(0x00, 0x01, 0x02), "nope"))
            }.close()
        }.exceptionOrNull()
        assertNotNull(error, "corrupt PKCS#12 must fail installMtls")
    }
}
