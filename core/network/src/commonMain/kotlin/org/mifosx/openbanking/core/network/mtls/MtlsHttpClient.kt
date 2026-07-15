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

/**
 * The mTLS client identity, loaded from `composeResources`.
 *
 * The JVM engine loads it into a [java.security.KeyStore]; the Darwin engine imports it via
 * `SecPKCS12Import`. Both consume the same PKCS#12 bundle (transport certificate + private key).
 */
class MtlsIdentity(
    val pkcs12: ByteArray,
    val pkcs12Password: String = "",
)

/**
 * Configures the engine's TLS layer to present [identity] as the client certificate, satisfying the
 * FAPI mutual-TLS requirement. Because a client certificate is presented during the TLS handshake
 * (inside the engine), this cannot be a request-pipeline plugin — it is an engine-config extension
 * meant to be called inside [httpClient]'s block alongside the other plugins:
 *
 * ```
 * httpClient {
 *     installMtls(identity)
 *     install(Auth) { bearer { ... } }
 *     install(ContentNegotiation) { json() }
 * }
 * ```
 *
 * Not supported on web (js/wasmJs) — a browser cannot present a client certificate; those actuals
 * throw [UnsupportedOperationException].
 */
expect fun HttpClientConfig<*>.installMtls(identity: MtlsIdentity)
