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

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64

internal suspend fun buildClientAssertion(
    clientId: String,
    kid: String,
    tokenUrl: String,
    nowEpochSeconds: Long,
    jti: String,
    privateKeyPem: String,
): String {
    val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    val provider = CryptographyProvider.Default.get(RSA.PSS)
    val jwtHeader = buildJsonObject {
        put("alg", "PS256")
        put("kid", kid)
    }.toString().encodeToByteArray()

    val jwtPayload = buildJsonObject {
        put("iss", clientId)
        put("sub", clientId)
        put("aud", tokenUrl)
        put("jti", jti)
        put("iat", nowEpochSeconds)
        put("exp", nowEpochSeconds + 60)
    }.toString().encodeToByteArray()

    val signingInput = "${base64Url.encode(jwtHeader)}.${base64Url.encode(jwtPayload)}"

    val privateKey = provider.privateKeyDecoder(digest = SHA256)
        .decodeFromByteArray(RSA.PrivateKey.Format.PEM.Generic, privateKeyPem.encodeToByteArray())

    val signature = privateKey.signatureGenerator()
        .generateSignature(signingInput.encodeToByteArray())

    return "$signingInput.${base64Url.encode(signature)}"
}
