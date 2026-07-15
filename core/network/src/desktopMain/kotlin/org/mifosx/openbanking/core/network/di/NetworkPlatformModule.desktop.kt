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

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mifosx.openbanking.core.network.certs.CertPaths
import org.mifosx.openbanking.core.network.mtls.MtlsIdentity

/** Desktop (JVM): reads the certificate material from the classpath (`desktopMain/resources/certs/`). */
actual val networkPlatformModule: Module = module {
    single { MtlsIdentity(pkcs12 = readClasspathCert(CertPaths.TRANSPORT_P12)) }
    single(named("hsbcSigningKey")) { readClasspathCert(CertPaths.SIGNING_KEY_PEM).decodeToString() }
}

internal fun readClasspathCert(name: String): ByteArray {
    val loader = Thread.currentThread().contextClassLoader ?: object {}.javaClass.classLoader
    return (
        loader?.getResourceAsStream(name)
            ?: error("Missing network certificate on the classpath: $name")
        ).use { it.readBytes() }
}
