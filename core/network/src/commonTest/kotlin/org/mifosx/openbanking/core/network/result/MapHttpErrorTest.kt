/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.result

import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MapHttpErrorTest {

    @Test
    fun `400 maps to BadRequest carrying the body`() {
        val error = mapHttpError(400, "bad-input")
        assertIs<NetworkError.Client.BadRequest>(error)
        assertEquals(400, error.statusCode)
        assertEquals("bad-input", error.body)
    }

    @Test
    fun `401 maps to Unauthorized carrying the body`() {
        val error = mapHttpError(401, """{"Code":"invalid_token"}""")
        assertIs<NetworkError.Client.Unauthorized>(error)
        assertEquals(401, error.statusCode)
        assertEquals("""{"Code":"invalid_token"}""", error.body)
    }

    @Test
    fun `403 maps to Forbidden`() {
        val error = mapHttpError(403, "consent revoked")
        assertIs<NetworkError.Client.Forbidden>(error)
        assertEquals(403, error.statusCode)
    }

    @Test
    fun `404 maps to NotFound`() {
        assertIs<NetworkError.Client.NotFound>(mapHttpError(404, null))
    }

    @Test
    fun `429 maps to RateLimited`() {
        assertIs<NetworkError.Client.RateLimited>(mapHttpError(429, null))
    }

    @Test
    fun `other 4xx maps to Client_Other with the actual code`() {
        val error = mapHttpError(418, "teapot")
        assertIs<NetworkError.Client.Other>(error)
        assertEquals(418, error.statusCode)
        assertEquals("teapot", error.body)
    }

    @Test
    fun `5xx maps to Server with the actual code`() {
        val error = mapHttpError(503, "unavailable")
        assertIs<NetworkError.Server>(error)
        assertEquals(503, error.statusCode)
        assertEquals("unavailable", error.body)
    }

    @Test
    fun `non-http code maps to Unknown`() {
        val error = mapHttpError(300, null)
        assertIs<NetworkError.Unknown>(error)
        assertNull(error.statusCode)
        assertNull(error.body)
    }
}
