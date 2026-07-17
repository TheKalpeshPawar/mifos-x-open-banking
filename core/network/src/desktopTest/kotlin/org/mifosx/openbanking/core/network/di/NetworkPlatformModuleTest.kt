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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Covers `readClasspathCert`, which is a resource loader — it does not parse or validate a
 * certificate, and cannot tell one from a text file.
 *
 * So these assert against a probe fixture this test owns. Naming the real `certs/…` paths, as this
 * previously did, made the result depend on `desktopTest/resources` happening to shadow
 * `desktopMain/resources` on the classpath: an accident of ordering, not a contract. A test that
 * green-lights on shadowing is worse than none — remove the fixtures and it silently starts
 * asserting against the **real** HSBC cert on a dev machine, and fails outright on CI.
 */
class NetworkPlatformModuleTest {

    @Test
    fun `readClasspathCert reads a resource off the classpath`() {
        assertEquals("probe", readClasspathCert(PROBE).decodeToString().trim())
    }

    @Test
    fun `readClasspathCert returns the raw bytes`() {
        assertTrue(readClasspathCert(PROBE).isNotEmpty())
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

    private companion object {
        const val PROBE = "classpath-probe.txt"
    }
}
