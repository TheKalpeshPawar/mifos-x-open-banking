/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.io.encoding.Base64

class PKCE {
    fun getCodeVerifier(): String {
        val byteArray = ByteArray(size = 48)
        val randomBytes = CryptographyRandom.nextBytes(byteArray)
        return Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(randomBytes)
    }

    suspend fun getCodeChallenge(verifier: String): String {
        val provider = CryptographyProvider.Default
        val hasher = provider.get(SHA256).hasher()
        val challengeString = hasher.hash(verifier.encodeToByteArray())

        return Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(challengeString)
    }
}
