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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.mifosx.openbanking.core.network.signPs256Bytes
import kotlin.io.encoding.Base64

internal const val CLAIM_IAT = "http://openbanking.org.uk/iat"
internal const val CLAIM_ISS = "http://openbanking.org.uk/iss"
internal const val CLAIM_TAN = "http://openbanking.org.uk/tan"

/** The Open Banking directory that vouches for the signing certificate. */
internal const val OPEN_BANKING_TRUST_ANCHOR = "openbanking.org.uk"

/**
 * The only value the Read/Write profile permits for `typ`.
 *
 * The profile is explicit: `typ` is optional, but "if it is specified, it must be set to the value
 * JOSE". `JWT` — the value a plain JWS library defaults to, and the one the sandbox's Postman
 * collection uses — is refused with `UK.OBIE.Signature.InvalidClaim` naming `typ`.
 */
private const val JOSE_TYPE = "JOSE"

private const val DETACHED_SEGMENT_COUNT = 3

private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

/**
 * Builds the `x-jws-signature` header value for an OBIE write request.
 *
 * The signature is an ordinary compact JWS whose payload segment is then removed, which is the
 * detached form RFC 7515 Appendix F describes and the Read/Write Data API profile requires.
 *
 * The header carries exactly what the v4.0.1 profile lists: `alg`, `kid`, the three
 * `openbanking.org.uk` private claims, and a `crit` naming those three. Two things it deliberately
 * does **not** carry, both learned from the bank refusing them:
 *
 * - **`typ` must be `JOSE`, not `JWT`.** See [JOSE_TYPE].
 * - **`b64` is not a v4.0.1 header field**, and must not appear in `crit`. Earlier revisions of the
 *   standard signed an unencoded payload under `b64: false`; v4.0.1 does not, so the payload is
 *   base64url-encoded like any other JWS before being detached.
 *
 * [payload] must be the exact string sent as the request body, or the signature covers different
 * bytes than the bank verifies.
 */
internal suspend fun detachedJwsSignature(
    payload: String,
    kid: String,
    signingKeyPem: String,
    issuer: String,
    issuedAtEpochSeconds: Long,
): String {
    val header = buildJsonObject {
        put("alg", "PS256")
        put("kid", kid)
        put("typ", JOSE_TYPE)
        put("cty", "application/json")
        put(CLAIM_IAT, issuedAtEpochSeconds)
        put(CLAIM_ISS, issuer)
        put(CLAIM_TAN, OPEN_BANKING_TRUST_ANCHOR)
        putJsonArray("crit") {
            add(CLAIM_IAT)
            add(CLAIM_ISS)
            add(CLAIM_TAN)
        }
    }

    val encodedHeader = base64Url.encode(header.toString().encodeToByteArray())
    val encodedPayload = base64Url.encode(payload.encodeToByteArray())
    val signature = signPs256Bytes("$encodedHeader.$encodedPayload", signingKeyPem)

    return "$encodedHeader..${base64Url.encode(signature)}"
}

/** Decodes a detached signature's header, for tests and diagnostics. */
internal fun detachedJwsHeader(signature: String): JsonObject {
    val segments = signature.split(".")
    require(segments.size == DETACHED_SEGMENT_COUNT) {
        "Expected a detached JWS with 3 segments, got ${segments.size}"
    }
    return Json.parseToJsonElement(base64Url.decode(segments[0]).decodeToString()) as JsonObject
}
