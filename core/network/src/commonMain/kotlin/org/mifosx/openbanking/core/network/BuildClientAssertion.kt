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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64

/**
 * Signs a JWS with `PS256` (RSA-PSS + SHA-256) — the FAPI signing algorithm — from a PEM private key.
 *
 * Produces the compact serialization `base64url(header).base64url(payload).base64url(signature)`.
 * Shared by the `private_key_jwt` client assertion and the authorization request object.
 */
internal suspend fun signPs256(
    header: JsonObject,
    payload: JsonObject,
    privateKeyPem: String,
): String {
    val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    val headerBytes = header.toString().encodeToByteArray()
    val payloadBytes = payload.toString().encodeToByteArray()
    val signingInput = "${base64Url.encode(headerBytes)}.${base64Url.encode(payloadBytes)}"

    return "$signingInput.${base64Url.encode(signPs256Bytes(signingInput, privateKeyPem))}"
}

/**
 * Signs an already-assembled JWS signing input.
 *
 * Split out because the detached signature OBIE requires for payment bodies sets `b64: false`, which
 * means the payload goes into the signing input raw rather than base64url-encoded — so it cannot use
 * [signPs256], which encodes both segments. Both still share one key-decoding path.
 */
internal suspend fun signPs256Bytes(
    signingInput: String,
    privateKeyPem: String,
): ByteArray {
    val privateKey = CryptographyProvider.Default.get(RSA.PSS)
        .privateKeyDecoder(digest = SHA256)
        .decodeFromByteArray(RSA.PrivateKey.Format.PEM.Generic, privateKeyPem.encodeToByteArray())

    return privateKey.signatureGenerator().generateSignature(signingInput.encodeToByteArray())
}

/**
 * Builds a FAPI `private_key_jwt` client assertion (a PS256-signed JWT) used to authenticate the TPP
 * at the token endpoint.
 */
internal suspend fun buildClientAssertion(
    clientId: String,
    kid: String,
    tokenUrl: String,
    nowEpochSeconds: Long,
    jti: String,
    privateKeyPem: String,
): String {
    val header = buildJsonObject {
        put("alg", "PS256")
        put("kid", kid)
    }
    val payload = buildJsonObject {
        put("iss", clientId)
        put("sub", clientId)
        put("aud", tokenUrl)
        put("jti", jti)
        put("iat", nowEpochSeconds)
        put("exp", nowEpochSeconds + 60)
    }
    return signPs256(header, payload, privateKeyPem)
}
