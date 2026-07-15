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

    private fun testPkcs12(): ByteArray =
        checkNotNull(this::class.java.getResourceAsStream("/test-identity.p12")) {
            "test-identity.p12 not found on the test classpath"
        }.readBytes()

    @Test
    fun `installMtls builds a client from a valid PKCS12 identity`() {
        // Exercises the whole JVM path: KeyStore load → KeyManagerFactory → TrustManagerFactory →
        // SSLContext → OkHttp sslSocketFactory. Building the client runs installMtls synchronously.
        val client = httpClient {
            installMtls(MtlsIdentity(testPkcs12(), "testpass"))
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
