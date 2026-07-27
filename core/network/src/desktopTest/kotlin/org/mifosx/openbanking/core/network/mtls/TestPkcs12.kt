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

import okhttp3.tls.HeldCertificate
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.cert.Certificate

/**
 * A throwaway self-signed mTLS identity, generated per test run.
 *
 * [installMtls] only requires bytes that load as a PKCS#12 holding a key plus a cert chain — it never
 * validates the issuer, so a committed `.p12` bought nothing that generating one does not, while
 * costing a binary blob of key material in git.
 *
 * RSA rather than okhttp's ECDSA P-256 default, matching the shape of the real HSBC transport cert.
 */
internal fun generateTestPkcs12(password: String = "testpass"): ByteArray {
    val held = HeldCertificate.Builder()
        .commonName("mtls-install-test")
        .rsa2048()
        .build()

    val store = KeyStore.getInstance("PKCS12").apply {
        load(null, null)
        setKeyEntry(
            "transport",
            held.keyPair.private,
            password.toCharArray(),
            arrayOf<Certificate>(held.certificate),
        )
    }

    return ByteArrayOutputStream().use { out ->
        store.store(out, password.toCharArray())
        out.toByteArray()
    }
}
