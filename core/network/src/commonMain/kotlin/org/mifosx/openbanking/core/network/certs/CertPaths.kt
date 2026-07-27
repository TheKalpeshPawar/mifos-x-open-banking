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

/**
 * Resource-relative names of the HSBC sandbox certificate material, loaded synchronously per platform
 * (Android assets, JVM classpath, iOS bundle) and injected via `networkPlatformModule`.
 *
 * These files are gitignored and supplied locally per developer / CI — they are NOT committed.
 * - [SIGNING_KEY_PEM] : the private key that signs the `private_key_jwt` client assertion.
 * - [TRANSPORT_P12]   : the mTLS client identity as PKCS#12 (transport certificate + private key).
 */
internal object CertPaths {
    const val SIGNING_KEY_PEM: String = "certs/signing_key.pem"
    const val TRANSPORT_P12: String = "certs/transport.p12"
}
