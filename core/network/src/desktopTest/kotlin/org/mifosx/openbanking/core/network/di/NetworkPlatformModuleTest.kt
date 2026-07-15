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

import org.mifosx.openbanking.core.network.certs.CertPaths
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NetworkPlatformModuleTest {

    // Throwaway self-signed fixtures live at src/desktopTest/resources/certs/ (committed).

    @Test
    fun `readClasspathCert reads the transport identity from the classpath`() {
        val bytes = readClasspathCert(CertPaths.TRANSPORT_P12)
        assertTrue(bytes.isNotEmpty(), "expected ${CertPaths.TRANSPORT_P12} to be read as non-empty bytes")
    }

    @Test
    fun `readClasspathCert reads the signing key from the classpath`() {
        val bytes = readClasspathCert(CertPaths.SIGNING_KEY_PEM)
        assertTrue(bytes.isNotEmpty(), "expected ${CertPaths.SIGNING_KEY_PEM} to be read as non-empty bytes")
    }

    @Test
    fun `readClasspathCert fails with a clear message when the resource is missing`() {
        val error = assertFailsWith<IllegalStateException> {
            readClasspathCert("certs/does-not-exist.p12")
        }
        assertTrue(
            error.message?.contains("certs/does-not-exist.p12") == true,
            "error message should name the missing resource",
        )
    }
}
