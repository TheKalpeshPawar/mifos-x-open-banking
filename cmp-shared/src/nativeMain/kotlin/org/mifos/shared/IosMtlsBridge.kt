/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifos.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.mifosx.openbanking.core.network.mtls.mtlsCredentialProvider
import platform.Foundation.NSData
import platform.Foundation.NSURLCredential
import platform.Foundation.create

/**
 * Wires the iOS mTLS client certificate into the shared module.
 *
 * Kotlin/Native's Foundation bindings can't cleanly build an [NSURLCredential] from a `SecIdentity`,
 * so the credential is built in Swift (via `SecPKCS12Import` + `URLCredential(identity:...)`). The iOS
 * app calls this at start-up, passing a builder that turns the PKCS#12 bytes + password into an
 * [NSURLCredential]; the `core/network` Darwin challenge handler then hands that credential back on
 * every client-certificate challenge. Until this is called, the handler defers to default handling and
 * no client certificate is presented.
 *
 * Exposed to Swift as `IosMtlsBridgeKt.registerMtlsCredentialProvider(build:)`. The PKCS#12 bytes are
 * copied to [NSData] here so Swift never handles a Kotlin `ByteArray`.
 */
@OptIn(ExperimentalForeignApi::class)
fun registerMtlsCredentialProvider(build: (NSData, String) -> NSURLCredential?) {
    mtlsCredentialProvider = { identity -> build(identity.pkcs12.toNSData(), identity.pkcs12Password) }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    }
