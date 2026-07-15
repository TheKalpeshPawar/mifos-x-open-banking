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

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.network.certs.CertPaths
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NetworkPlatformModuleAndroidTest {

    // Throwaway self-signed fixtures live at src/androidUnitTest/assets/certs/ (committed).

    @Test
    fun readAssetCertReadsTheTransportIdentityFromAssets() {
        val context = RuntimeEnvironment.getApplication()
        val bytes = context.readAssetCert(CertPaths.TRANSPORT_P12)
        assertTrue("expected ${CertPaths.TRANSPORT_P12} to be read as non-empty bytes", bytes.isNotEmpty())
    }
}
