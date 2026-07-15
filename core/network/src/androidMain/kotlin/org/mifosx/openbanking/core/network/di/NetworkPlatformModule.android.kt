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

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mifosx.openbanking.core.network.certs.CertPaths
import org.mifosx.openbanking.core.network.mtls.MtlsIdentity

/** Android: reads the certificate material from the app's assets (`androidMain/assets/certs/`). */
actual val networkPlatformModule: Module = module {
    single { MtlsIdentity(pkcs12 = androidContext().readAssetCert(CertPaths.TRANSPORT_P12)) }
    single(named("hsbcSigningKey")) { androidContext().readAssetCert(CertPaths.SIGNING_KEY_PEM).decodeToString() }
}

internal fun Context.readAssetCert(name: String): ByteArray = assets.open(name).use { it.readBytes() }
