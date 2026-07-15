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

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFArrayRefVar
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSURLAuthenticationMethodClientCertificate
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLCredentialPersistenceForSession
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.create
import platform.Security.SecIdentityRef
import platform.Security.SecPKCS12Import
import platform.Security.errSecSuccess
import platform.Security.kSecImportItemIdentity

/**
 * iOS mTLS: answers the server's client-certificate challenge with an identity imported from the
 * PKCS#12 bundle via `SecPKCS12Import`, installed on the Darwin engine's challenge handler.
 *
 * NOTE: this Security-framework cinterop is authored on a non-Apple host and has NOT been compiled
 * or run — verify + adjust symbols when building on macOS.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun HttpClientConfig<*>.installMtls(identity: MtlsIdentity) {
    @Suppress("UNCHECKED_CAST")
    (this as HttpClientConfig<DarwinClientEngineConfig>).engine {
        handleChallenge { _, _, challenge, completionHandler ->
            if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodClientCertificate) {
                val credential = clientCredential(identity)
                if (credential != null) {
                    completionHandler(NSURLSessionAuthChallengeUseCredential, credential)
                } else {
                    completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
                }
            } else {
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun clientCredential(identity: MtlsIdentity): NSURLCredential? = memScoped {
    val data: NSData = identity.pkcs12.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = identity.pkcs12.size.toULong())
    }

    val options = CFDictionaryCreateMutable(kCFAllocatorDefault, 1, null, null)
    val passphrase = CFBridgingRetain(identity.pkcs12Password) as CFStringRef
    // kSecImportExportPassphrase key → the p12 passphrase.
    platform.CoreFoundation.CFDictionarySetValue(
        options,
        platform.Security.kSecImportExportPassphrase,
        passphrase,
    )

    val items = alloc<CFArrayRefVar>()
    val status = SecPKCS12Import(CFBridgingRetain(data) as platform.CoreFoundation.CFDataRef, options, items.ptr)
    if (status != errSecSuccess) return@memScoped null

    val array = items.value ?: return@memScoped null
    if (CFArrayGetCount(array) == 0L) return@memScoped null

    val firstItem = CFArrayGetValueAtIndex(array, 0)?.reinterpret<CFDictionaryRef>() ?: return@memScoped null
    val identityRef: SecIdentityRef = CFDictionaryGetValue(firstItem, kSecImportItemIdentity)
        ?.reinterpret() ?: return@memScoped null

    NSURLCredential.credentialWithIdentity(
        identity = identityRef,
        certificates = null,
        persistence = NSURLCredentialPersistenceForSession,
    )
}
