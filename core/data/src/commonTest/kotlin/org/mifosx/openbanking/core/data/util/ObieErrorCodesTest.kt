/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.util

import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The real body HSBC returns for an unsupported product, captured from the sandbox verbatim. */
private const val U000_BODY = """
{"Code":"400","Id":"842f0682-ba4a-4f17-9107-a5ce8b98fdbd","Message":"Bad Request",
 "Errors":[{"ErrorCode":"U000",
            "Message":"This action is not allowed on the account type in the request"}]}
"""

class ObieErrorCodesTest {

    @Test
    fun aBadRequestCarryingU000IsUnsupported() {
        assertTrue(NetworkError.Client.BadRequest(body = U000_BODY).isUnsupportedForProduct())
    }

    @Test
    fun aBadRequestWithADifferentErrorCodeIsNotUnsupported() {
        val body = """{"Code":"400","Errors":[{"ErrorCode":"UK.OBIE.Field.Invalid"}]}"""
        assertFalse(NetworkError.Client.BadRequest(body = body).isUnsupportedForProduct())
    }

    /**
     * The status is half the guard. HSBC's own error catalogue uses `U000` on 403 and 500 for
     * unrelated situations, so matching the code alone would mistake an outage for an unsupported
     * product and permanently hide a feature that works.
     */
    @Test
    fun aForbiddenCarryingU000IsNotUnsupported() {
        assertFalse(NetworkError.Client.Forbidden(body = U000_BODY).isUnsupportedForProduct())
    }

    @Test
    fun aNullOrBlankBodyIsNotUnsupportedAndDoesNotThrow() {
        assertFalse(NetworkError.Client.BadRequest(body = null).isUnsupportedForProduct())
        assertFalse(NetworkError.Client.BadRequest(body = "").isUnsupportedForProduct())
    }

    /**
     * Gateways return HTML, and truncated payloads happen. Falling back to a substring keeps a
     * malformed body from reading as "supported" and sending the user back into a call that cannot
     * succeed.
     */
    @Test
    fun aNonJsonBodyFallsBackToASubstringMatch() {
        assertTrue(
            NetworkError.Client.BadRequest(body = "<html>U000 not allowed</html>")
                .isUnsupportedForProduct(),
        )
        assertFalse(
            NetworkError.Client.BadRequest(body = "<html>502 Bad Gateway</html>")
                .isUnsupportedForProduct(),
        )
    }

    @Test
    fun obieMessageReturnsTheBanksOwnText() {
        assertEquals(
            "This action is not allowed on the account type in the request",
            NetworkError.Client.BadRequest(body = U000_BODY).obieMessage(),
        )
    }

    @Test
    fun obieMessagePrefersTheNestedErrorOverTheTopLevelStatusPhrase() {
        // The top-level "Message" is only ever the status phrase; the useful text is in Errors[].
        val body = """{"Code":"400","Message":"Bad Request","Errors":[{"Message":"Specific reason"}]}"""
        assertEquals("Specific reason", NetworkError.Client.BadRequest(body = body).obieMessage())
    }

    @Test
    fun obieMessageIsNullWhenTheBodyCarriesNone() {
        assertNull(NetworkError.Client.BadRequest(body = null).obieMessage())
        assertNull(NetworkError.Client.BadRequest(body = "<html>oops</html>").obieMessage())
        assertNull(NetworkError.Client.BadRequest(body = """{"Code":"400"}""").obieMessage())
    }

    @Test
    fun theRemoteExceptionWrapperIsUnwrapped() {
        val wrapped: Throwable = RemoteException(NetworkError.Client.BadRequest(body = U000_BODY))
        assertTrue(wrapped.isUnsupportedForProduct())
        assertEquals(
            "This action is not allowed on the account type in the request",
            wrapped.obieMessage(),
        )
    }

    /**
     * The whole point of the `ResultExtensions` change: a 400 that explains itself should surface
     * the bank's words, not our generic stand-in.
     */
    @Test
    fun toThrowableSurfacesTheBanksMessageForAnExplainedBadRequest() {
        val thrown = NetworkError.Client.BadRequest(body = U000_BODY).toThrowable()
        assertEquals(
            "This action is not allowed on the account type in the request",
            thrown.message,
        )
    }

    @Test
    fun toThrowableFallsBackToTheGenericMessageWhenTheBodyExplainsNothing() {
        val thrown = NetworkError.Client.BadRequest(body = null).toThrowable()
        assertEquals("Something went wrong with your request. Please try again.", thrown.message)
    }
}
