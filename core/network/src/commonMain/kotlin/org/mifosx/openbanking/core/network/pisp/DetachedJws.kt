/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.pisp

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mifosx.openbanking.core.network.signPs256

private const val DETACHED_SEGMENT_COUNT = 3

/**
 * Builds the `x-jws-signature` header value for an OBIE write request.
 *
 * The signature is produced as an ordinary compact JWS over [payload] and then detached by dropping
 * the payload segment, yielding `base64url(header)..base64url(signature)`.
 *
 * The header carries only `typ`/`alg`/`kid`. That is narrower than the OBIE Read/Write standard,
 * which specifies `b64: false` plus the `http://openbanking.org.uk/{iat,iss,tan}` claims listed in
 * `crit` — but it is what the HSBC sandbox verifies against, per the construction in its published
 * Postman collection. Signing the raw payload under `b64: false` produces a signature this sandbox
 * rejects with `U019`.
 *
 * [payload] must be the same [JsonObject] whose `toString()` is sent as the request body, so the
 * bytes signed and the bytes transmitted cannot diverge.
 */
internal suspend fun detachedJwsSignature(
    payload: JsonObject,
    kid: String,
    signingKeyPem: String,
): String {
    val header = buildJsonObject {
        put("typ", "JWT")
        put("alg", "PS256")
        put("kid", kid)
    }
    val segments = signPs256(header, payload, signingKeyPem).split(".")
    require(segments.size == DETACHED_SEGMENT_COUNT) {
        "Expected a compact JWS with 3 segments, got ${segments.size}"
    }
    return "${segments[0]}..${segments[2]}"
}
