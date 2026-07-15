/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.certs

import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.mifosx.openbanking.core.network.generated.resources.Res

/**
 * Paths (relative to `composeResources`) of the HSBC sandbox certificate material.
 *
 * These files are gitignored and supplied locally per developer / CI — they are NOT committed.
 * - [SIGNING_KEY_PEM]   : the private key used to sign the private_key_jwt client assertion.
 * - [TRANSPORT_CERT_PEM]: the mTLS client certificate (Transport.crt).
 * - [TRANSPORT_KEY_PEM] : the private key backing the mTLS client certificate (JVM engines).
 * - [TRANSPORT_P12]     : the mTLS client identity as PKCS#12 (Darwin/iOS engine).
 */
internal object CertPaths {
    const val SIGNING_KEY_PEM: String = "files/certs/signing_key.pem"
    const val TRANSPORT_CERT_PEM: String = "files/certs/transport.pem"
    const val TRANSPORT_KEY_PEM: String = "files/certs/transport_key.pem"
    const val TRANSPORT_P12: String = "files/certs/transport.p12"
}

/** Reads a bundled certificate/key file from `composeResources` as raw bytes. */
@OptIn(ExperimentalResourceApi::class)
internal suspend fun loadCertBytes(path: String): ByteArray = Res.readBytes(path)
