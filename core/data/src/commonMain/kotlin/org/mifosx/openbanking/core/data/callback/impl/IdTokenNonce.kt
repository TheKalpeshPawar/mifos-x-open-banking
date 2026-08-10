/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.callback.impl

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Padding is optional because a JWT's base64url segments are unpadded per RFC 7515 §2, while
 * Kotlin's [Base64.UrlSafe] demands padding by default and throws without it. Decoding a real HSBC
 * `id_token` with the strict variant therefore threw on every genuine consent and reported it as a
 * nonce mismatch.
 */
@OptIn(ExperimentalEncodingApi::class)
private val jwtBase64: Base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

private const val MIN_JWT_SEGMENTS = 2

/**
 * Whether the `id_token`'s `nonce` claim fails to match the one issued for this authorisation.
 *
 * Fails closed: a missing, malformed or undecodable token counts as a mismatch. Shared by the
 * sign-in and payment authorisation legs so the replay check cannot drift between them.
 */
@OptIn(ExperimentalEncodingApi::class)
@Suppress("ReturnCount")
internal fun idTokenNonceMismatch(idToken: String?, expectedNonce: String): Boolean {
    if (idToken == null) return true
    return try {
        val parts = idToken.split(".")
        if (parts.size < MIN_JWT_SEGMENTS) return true
        val payload = jwtBase64.decode(parts[1]).decodeToString()
        val nonce = Json.parseToJsonElement(payload).jsonObject["nonce"]?.jsonPrimitive?.content
        nonce != expectedNonce
    } catch (_: Exception) {
        true
    }
}
