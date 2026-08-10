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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.mifosx.openbanking.core.network.TestSigningKey
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val KID = "test-kid-1"
private const val ISSUER = "mifos_init_00000/0000000000000000000000"
private const val IAT = 1_754_000_000L
private const val PAYLOAD = """{"Data":{"Initiation":{"InstructionIdentification":"MFX1"}}}"""

/**
 * Covers [detachedJwsSignature]: the `x-jws-signature` value the two PISP write calls carry.
 *
 * The signature bytes are never asserted — PS256 is RSA-PSS, which salts randomly, so two signings
 * of identical input differ. What is asserted is the JOSE header the bank parses, against the
 * header table in the v4.0.1 Read/Write Data API profile.
 *
 * The header this produces was arrived at empirically: the sandbox refuses `typ: "JWT"` with
 * `UK.OBIE.Signature.InvalidClaim` naming `typ`, and the profile resolves why — `typ` is optional
 * but must be `JOSE` when present. These cases exist to stop that being reintroduced.
 */
class DetachedJwsTest {

    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    private suspend fun signature(payload: String = PAYLOAD): String = detachedJwsSignature(
        payload = payload,
        kid = KID,
        signingKeyPem = TestSigningKey.pem(),
        issuer = ISSUER,
        issuedAtEpochSeconds = IAT,
    )

    @Test
    fun `detaches the payload leaving an empty middle segment`() = runTest {
        val segments = signature().split(".")

        assertEquals(3, segments.size)
        assertEquals("", segments[1])
        assertTrue(segments[0].isNotBlank())
        assertTrue(segments[2].isNotBlank())
    }

    @Test
    fun `header declares the JOSE type and the PS256 algorithm`() = runTest {
        val header = detachedJwsHeader(signature())

        assertEquals("JOSE", header.getValue("typ").jsonPrimitive.content)
        assertEquals("PS256", header.getValue("alg").jsonPrimitive.content)
        assertEquals(KID, header.getValue("kid").jsonPrimitive.content)
        assertEquals("application/json", header.getValue("cty").jsonPrimitive.content)
    }

    @Test
    fun `header carries the three open banking claims`() = runTest {
        val header = detachedJwsHeader(signature())

        assertEquals(IAT, header.getValue(CLAIM_IAT).jsonPrimitive.content.toLong())
        assertEquals(ISSUER, header.getValue(CLAIM_ISS).jsonPrimitive.content)
        assertEquals(OPEN_BANKING_TRUST_ANCHOR, header.getValue(CLAIM_TAN).jsonPrimitive.content)
    }

    /**
     * The profile requires `crit` to name exactly the three `openbanking.org.uk` claims, marking
     * them as extensions a validator must understand rather than ignore.
     */
    @Test
    fun `crit names exactly the three claims a validator must process`() = runTest {
        val crit = detachedJwsHeader(signature())
            .getValue("crit")
            .jsonArray
            .map { it.jsonPrimitive.content }

        assertEquals(listOf(CLAIM_IAT, CLAIM_ISS, CLAIM_TAN), crit)
    }

    /**
     * `b64` is not a field in the v4.0.1 header table. Earlier revisions signed an unencoded payload
     * under `b64: false`; this one does not, so the flag must be absent from both the header and
     * `crit` — a `crit` entry the header does not define is itself a malformed signature.
     */
    @Test
    fun `header omits the b64 flag that v4 0 1 removed`() = runTest {
        val header = detachedJwsHeader(signature())
        val crit = header.getValue("crit").jsonArray.map { it.jsonPrimitive.content }

        assertFalse("b64" in header.keys)
        assertFalse("b64" in crit)
    }

    /**
     * The payload is base64url-encoded before signing, which is what dropping `b64: false` means in
     * practice. Encoding it here and finding it absent from the transmitted value is what proves the
     * signature is detached rather than merely compact.
     */
    @Test
    fun `signs the encoded payload and then removes it`() = runTest {
        val encodedPayload = base64Url.encode(PAYLOAD.encodeToByteArray())

        val signature = signature()

        assertFalse(encodedPayload in signature)
        assertEquals("", signature.split(".")[1])
    }

    @Test
    fun `a different payload produces a different signature`() = runTest {
        val first = signature()
        val second = signature(payload = """{"Data":{"Initiation":{"InstructionIdentification":"MFX2"}}}""")

        assertEquals(detachedJwsHeader(first), detachedJwsHeader(second))
        assertTrue(first.substringAfterLast(".") != second.substringAfterLast("."))
    }
}
