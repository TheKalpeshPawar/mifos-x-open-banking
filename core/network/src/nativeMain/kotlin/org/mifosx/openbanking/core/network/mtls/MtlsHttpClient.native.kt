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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.Foundation.NSURLAuthenticationMethodClientCertificate
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential

/**
 * Builds the iOS mTLS client-certificate [NSURLCredential] from the PKCS#12 bundle.
 *
 * Kotlin/Native's Foundation bindings do not expose `NSURLCredential.credentialWithIdentity(...)`,
 * so the credential cannot be constructed from a `SecIdentity` in Kotlin — the same limitation the
 * `kmm_mtls_sample` project works around by building the credential in Swift and handing it back to
 * the shared module. The iOS app's Swift layer is expected to build the credential (via
 * `SecPKCS12Import` + `URLCredential(identity:certificates:persistence:)`) and register it here at
 * start-up; on any target where it is left unset the Darwin challenge handler defers to default
 * handling.
 */
var mtlsCredentialProvider: ((MtlsIdentity) -> NSURLCredential?)? = null

/**
 * iOS mTLS: answers the server's client-certificate challenge with the credential produced by
 * [mtlsCredentialProvider], installed on the Darwin engine's challenge handler.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun HttpClientConfig<*>.installMtls(identity: MtlsIdentity) {
    @Suppress("UNCHECKED_CAST")
    (this as HttpClientConfig<DarwinClientEngineConfig>).engine {
        handleChallenge { _, _, challenge, completionHandler ->
            val isClientCertChallenge = challenge.protectionSpace.authenticationMethod ==
                NSURLAuthenticationMethodClientCertificate
            val credential = if (isClientCertChallenge) mtlsCredentialProvider?.invoke(identity) else null
            if (credential != null) {
                completionHandler(NSURLSessionAuthChallengeUseCredential.convert(), credential)
            } else {
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling.convert(), null)
            }
        }
    }
}
