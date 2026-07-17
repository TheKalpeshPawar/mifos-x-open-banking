/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256

/**
 * An ephemeral PS256 signing key, generated in-process.
 *
 * [signPs256] needs *a* PKCS#8 PEM RSA key bound to SHA-256; no test asserts the signature bytes
 * against a fixed value, so a committed key never bought anything a generated one does not. Keeping
 * one in the repo did cost something, though: a fixture that is gitignored passes locally and fails
 * in CI, and one that is force-committed is signing material in git forever.
 *
 * 2048 bits rather than the library's 4096 default: keygen is the only slow step here, and these
 * tests assert JWS *structure*, not cryptographic strength.
 *
 * Cached per process, so the cost is paid once per test run rather than once per class. Deliberately
 * not `by lazy`: [dev.whyoleg.cryptography.materials.key.KeyGenerator.generateKey] and
 * `encodeToByteArray` are `suspend`, and their blocking variants throw on some providers. The
 * unsynchronised read is fine — a lost race costs one redundant keygen and nothing else.
 */
object TestSigningKey {

    private var cached: String? = null

    suspend fun pem(): String = cached ?: generate().also { cached = it }

    /**
     * `digest = SHA256` is load-bearing, not decoration: production decodes with
     * `privateKeyDecoder(digest = SHA256)` (`BuildClientAssertion.kt`), and providers bind the digest
     * into the key at generation. A mismatch fails at decode time.
     */
    private suspend fun generate(): String = CryptographyProvider.Default
        .get(RSA.PSS)
        .keyPairGenerator(keySize = 2048.bits, digest = SHA256)
        .generateKey()
        .privateKey
        .encodeToByteArray(RSA.PrivateKey.Format.PEM.Generic)
        .decodeToString()
}
