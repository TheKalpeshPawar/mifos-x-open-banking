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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The Android half of the cert loader: `readAssetCert` reads from APK assets rather than the JVM
 * classpath, so it needs its own cover.
 *
 * Asserts against a probe fixture this test owns, for the same reason as the desktop twin — the
 * loader cannot tell a certificate from a text file, and naming a real `certs/…` path would only
 * couple the result to which assets directory wins.
 */
@RunWith(RobolectricTestRunner::class)
class NetworkPlatformModuleAndroidTest {

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun readAssetCertReadsAResourceFromAssets() {
        assertEquals("probe", context.readAssetCert(PROBE).decodeToString().trim())
    }

    @Test
    fun readAssetCertReturnsTheRawBytes() {
        assertTrue(context.readAssetCert(PROBE).isNotEmpty())
    }

    private companion object {
        const val PROBE = "classpath-probe.txt"
    }
}
