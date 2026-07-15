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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mifosx.openbanking.core.network.certs.CertPaths
import org.mifosx.openbanking.core.network.mtls.MtlsIdentity
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/**
 * iOS: reads the certificate material from the app bundle (add the files as bundle resources under a
 * `certs/` folder reference in the iOS app target).
 *
 * NOTE: this Foundation lookup is authored on a non-Apple host and has NOT been compiled or run —
 * verify the bundle path resolution + `NSData` copy when building on macOS.
 */
actual val networkPlatformModule: Module = module {
    single { MtlsIdentity(pkcs12 = readBundleCert(CertPaths.TRANSPORT_P12)) }
    single(named("hsbcSigningKey")) { readBundleCert(CertPaths.SIGNING_KEY_PEM).decodeToString() }
}

@OptIn(ExperimentalForeignApi::class)
private fun readBundleCert(name: String): ByteArray {
    val directory = name.substringBeforeLast('/', missingDelimiterValue = "")
    val fileName = name.substringAfterLast('/')
    val resource = fileName.substringBeforeLast('.')
    val extension = fileName.substringAfterLast('.')

    val path = NSBundle.mainBundle.pathForResource(
        name = resource,
        ofType = extension,
        inDirectory = directory.ifEmpty { null },
    ) ?: error("Missing network certificate in the app bundle: $name")

    val data = NSData.dataWithContentsOfFile(path)
        ?: error("Unable to read network certificate from the app bundle: $name")

    return ByteArray(data.length.toInt()).apply {
        if (isNotEmpty()) {
            usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
        }
    }
}
