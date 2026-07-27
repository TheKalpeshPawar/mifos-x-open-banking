/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256

/**
 * An ephemeral PS256 signing key, generated in-process.
 *
 * A deliberate twin of `:core:network`'s `TestSigningKey` — Gradle test source sets are not shared
 * between modules, and a whole `:core:testing` module is not worth its build wiring to hoist thirty
 * logic-free lines for two consumers. Revisit if a third appears.
 *
 * The signing paths reached from here (`OAuth.clientCredentialsToken`, `exchangeAuthorizationCode`,
 * `generateConsentAuthorizationUrl`) need *a* PKCS#8 PEM RSA key bound to SHA-256; nothing asserts
 * the signature bytes. Generating it is what lets these tests run in CI at all: the fixture they used
 * to read was gitignored, so they passed here and would have failed there.
 *
 * See the sibling for why this is cached but not `by lazy`.
 */
object TestSigningKey {

    private var cached: String? = null

    suspend fun pem(): String = cached ?: generate().also { cached = it }

    private suspend fun generate(): String = CryptographyProvider.Default
        .get(RSA.PSS)
        .keyPairGenerator(keySize = 2048.bits, digest = SHA256)
        .generateKey()
        .privateKey
        .encodeToByteArray(RSA.PrivateKey.Format.PEM.Generic)
        .decodeToString()
}
