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
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ResultExtensionsTest {

    private fun messageOf(error: NetworkError): String {
        val throwable = error.toThrowable()
        assertTrue(throwable is RemoteException)
        assertSame(error, throwable.networkError)
        return throwable.message ?: ""
    }

    @Test
    fun `each network error maps to its user facing message`() {
        assertEquals(
            "Something went wrong with your request. Please try again.",
            messageOf(NetworkError.Client.BadRequest()),
        )
        assertEquals(
            "The information you're looking for couldn't be found.",
            messageOf(NetworkError.Client.NotFound()),
        )
        assertEquals(
            "You need to sign in to access this content.",
            messageOf(NetworkError.Client.Unauthorized()),
        )
        assertEquals(
            "You don't have permission to access this. Your consent may have been revoked.",
            messageOf(NetworkError.Client.Forbidden()),
        )
        assertEquals(
            "You're doing that too often. Please wait a moment and try again.",
            messageOf(NetworkError.Client.RateLimited()),
        )
        assertEquals(
            "Something went wrong with your request. Please try again.",
            messageOf(NetworkError.Client.Other(statusCode = 418)),
        )
        assertEquals(
            "We're experiencing technical difficulties. Please try again later.",
            messageOf(NetworkError.Server(statusCode = 500)),
        )
        assertEquals(
            "We received unexpected data. Please try refreshing the app.",
            messageOf(NetworkError.Serialization(cause = RuntimeException("boom"))),
        )
        assertEquals(
            "Please check your connection and try again.",
            messageOf(NetworkError.Network(cause = RuntimeException("offline"))),
        )
        assertEquals(
            "Something unexpected happened. Please try again.",
            messageOf(NetworkError.Unknown()),
        )
    }

    @Test
    fun `remote exception default message prefers body then cause then fallback`() {
        val withBody = RemoteException(NetworkError.Client.BadRequest(body = "field missing"))
        assertEquals("field missing", withBody.message)

        val withCause = RemoteException(NetworkError.Network(cause = RuntimeException("socket closed")))
        assertEquals("socket closed", withCause.message)

        val withNeither = RemoteException(NetworkError.Unknown())
        assertEquals("Network error", withNeither.message)
    }
}
