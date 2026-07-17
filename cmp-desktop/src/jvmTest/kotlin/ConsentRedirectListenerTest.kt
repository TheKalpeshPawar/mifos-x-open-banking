/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mifosx.openbanking.core.data.callback.ConsentRedirectBus
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the desktop redirect path over a real HTTP request to the real listener.
 *
 * Desktop is the platform with no custom-scheme handler, so this loopback hop is the only way the
 * consent can complete there — and none of the shared tests touch it.
 */
class ConsentRedirectListenerTest {

    @BeforeTest
    fun startListener() {
        ConsentRedirectListener.start()
    }

    @AfterTest
    fun stopListener() {
        ConsentRedirectListener.stop()
    }

    private fun get(path: String): Pair<Int, String> {
        val connection = URI("http://127.0.0.1:${ConsentRedirectListener.PORT}$path")
            .toURL()
            .openConnection() as HttpURLConnection
        return try {
            val status = connection.responseCode
            val body = (if (status < 400) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            status to body
        } finally {
            connection.disconnect()
        }
    }

    @Test
    fun `a relayed callback is published to the bus`() = runBlocking {
        val (status, _) = get("/callback?code=desktop-code&id_token=ey.J.sig&state=st-1")

        assertEquals(200, status)

        val published = withTimeout(TIMEOUT_MS) { ConsentRedirectBus.redirects.first() }
        assertEquals(
            "org.mifosx.openbanking://callback?code=desktop-code&id_token=ey.J.sig&state=st-1",
            published,
            "the loopback query is normalised to the same shape the mobile custom scheme delivers, " +
                "so the data layer parses one form",
        )
    }

    @Test
    fun `the browser is told it can close the tab`() {
        val (status, body) = get("/callback?code=any&state=st-1")

        assertEquals(200, status)
        assertTrue(body.contains("close this tab"), "the PSU is left staring at a blank page")
    }

    @Test
    fun `a callback with no parameters is rejected rather than published as empty`() {
        val (status, _) = get("/callback")

        assertEquals(404, status)
    }

    @Test
    fun `an error response relays through untouched`() = runBlocking {
        get("/callback?error=access_denied&state=st-1")

        val published = withTimeout(TIMEOUT_MS) { ConsentRedirectBus.redirects.first() }
        assertTrue(published.contains("error=access_denied"))
    }

    @Test
    fun `starting twice is a no-op rather than a port clash`() {
        ConsentRedirectListener.start()
        ConsentRedirectListener.start()

        val (status, _) = get("/callback?code=still-listening&state=st-1")
        assertEquals(200, status)
    }

    @Test
    fun `stop releases the port so a later start can rebind`() {
        ConsentRedirectListener.stop()

        assertTrue(ConsentRedirectListener.start())

        val (status, _) = get("/callback?code=rebound&state=st-1")
        assertEquals(200, status)
    }

    @Test
    fun `start reports failure rather than throwing when the port is taken`() {
        // Non-fatal by design — a second copy of the app must still launch. But it must not look
        // like success: the PSU would authorise, the bridge page would relay to a dead port, and the
        // app would wait forever on a callback that can never arrive.
        ConsentRedirectListener.stop()

        ServerSocket(ConsentRedirectListener.PORT, 0, InetAddress.getLoopbackAddress()).use {
            assertFalse(ConsentRedirectListener.start(), "a taken port must not report success")
        }
    }

    @Test
    fun `start reports success and stays idempotent when the port is free`() {
        ConsentRedirectListener.stop()

        assertTrue(ConsentRedirectListener.start())
        assertTrue(ConsentRedirectListener.start(), "a second call is a no-op, not a failure")
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
