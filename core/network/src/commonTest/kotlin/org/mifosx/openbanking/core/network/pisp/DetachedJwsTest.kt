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

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mifosx.openbanking.core.network.TestSigningKey
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val KID = "test-kid-1"

/**
 * Covers [detachedJwsSignature]: the `x-jws-signature` value the two PISP write calls carry.
 *
 * The signature bytes themselves are never asserted — PS256 is RSA-PSS, which salts randomly, so
 * two signings of identical input differ. These assert the structure the bank parses.
 */
class DetachedJwsTest {

    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    @Test
    fun `detaches the payload leaving an empty middle segment`() = runTest {
        val signature = detachedJwsSignature(
            payload = buildJsonObject { put("Data", "x") },
            kid = KID,
            signingKeyPem = TestSigningKey.pem(),
        )

        val segments = signature.split(".")
        assertEquals(3, segments.size)
        assertEquals("", segments[1])
        assertTrue(segments[0].isNotBlank())
        assertTrue(segments[2].isNotBlank())
    }

    @Test
    fun `header carries typ alg and kid and nothing else`() = runTest {
        val signature = detachedJwsSignature(
            payload = buildJsonObject { put("Data", "x") },
            kid = KID,
            signingKeyPem = TestSigningKey.pem(),
        )

        val header = Json.parseToJsonElement(
            base64Url.decode(signature.substringBefore(".")).decodeToString(),
        ).jsonObject

        assertEquals("JWT", header.getValue("typ").jsonPrimitive.content)
        assertEquals("PS256", header.getValue("alg").jsonPrimitive.content)
        assertEquals(KID, header.getValue("kid").jsonPrimitive.content)
        assertEquals(setOf("typ", "alg", "kid"), header.keys)
    }

    /**
     * The OBIE standard would additionally require `b64: false` plus the
     * `http://openbanking.org.uk/{iat,iss,tan}` claims listed in `crit`. HSBC's sandbox verifies the
     * narrower header its own Postman collection builds, and rejects the standard one with `U019`,
     * so their absence is the contract here rather than an omission.
     */
    @Test
    fun `header omits the obie crit claims the sandbox does not verify`() = runTest {
        val signature = detachedJwsSignature(
            payload = buildJsonObject { put("Data", "x") },
            kid = KID,
            signingKeyPem = TestSigningKey.pem(),
        )

        val header = Json.parseToJsonElement(
            base64Url.decode(signature.substringBefore(".")).decodeToString(),
        ).jsonObject

        assertTrue("b64" !in header.keys)
        assertTrue("crit" !in header.keys)
    }
}
