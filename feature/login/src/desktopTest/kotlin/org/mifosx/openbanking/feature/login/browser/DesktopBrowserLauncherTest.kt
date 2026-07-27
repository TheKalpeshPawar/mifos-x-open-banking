/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login.browser

import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val URL = "https://sandbox.test/auth"

class DesktopBrowserLauncherTest {

    private fun launcher(
        osName: String = "Linux",
        awtCanBrowse: () -> Boolean = { false },
        awtBrowse: (URI) -> Unit = { error("AWT should not be reached") },
        exec: (List<String>) -> Int = { 0 },
    ) = DesktopBrowserLauncher(osName, awtBrowse, awtCanBrowse, exec)

    @Test
    fun `uses AWT when the BROWSE action is supported`() {
        var browsed: URI? = null
        var execCalled = false

        launcher(
            awtCanBrowse = { true },
            awtBrowse = { browsed = it },
            exec = {
                execCalled = true
                0
            },
        ).launch(URL)

        assertEquals(URI(URL), browsed)
        assertTrue(!execCalled, "the platform opener is a fallback, not an unconditional second call")
    }

    /**
     * The reported Wayland crash: AWT reports a Desktop instance but not the BROWSE action, so the
     * old `Desktop.isDesktopSupported()` guard let `browse()` through to throw.
     */
    @Test
    fun `falls back to xdg-open on Linux when AWT cannot browse`() {
        var command: List<String>? = null

        launcher(osName = "Linux", awtCanBrowse = { false }, exec = {
            command = it
            0
        }).launch(URL)

        assertEquals(listOf("xdg-open", URL), command)
    }

    @Test
    fun `falls back to the platform opener when AWT claims support but then throws`() {
        var command: List<String>? = null

        launcher(
            osName = "Linux",
            awtCanBrowse = { true },
            awtBrowse = { throw UnsupportedOperationException("The BROWSE action is not supported") },
            exec = {
                command = it
                0
            },
        ).launch(URL)

        assertEquals(listOf("xdg-open", URL), command)
    }

    @Test
    fun `throws when AWT is unavailable and the opener exits non-zero`() {
        val e = assertFailsWith<BrowserLaunchException> {
            launcher(osName = "Linux", awtCanBrowse = { false }, exec = { 1 }).launch(URL)
        }
        assertTrue(
            e.message.orEmpty().contains("xdg-open exited 1"),
            "the failure must name what was tried so it is diagnosable: ${e.message}",
        )
    }

    @Test
    fun `throws rather than silently succeeding when the opener is missing`() {
        assertFailsWith<BrowserLaunchException> {
            launcher(
                osName = "Linux",
                awtCanBrowse = { false },
                exec = { throw java.io.IOException("xdg-open not found") },
            ).launch(URL)
        }
    }

    @Test
    fun `throws on an OS with no known opener`() {
        val e = assertFailsWith<BrowserLaunchException> {
            launcher(osName = "SomeFutureOS", awtCanBrowse = { false }).launch(URL)
        }
        assertTrue(e.message.orEmpty().contains("SomeFutureOS"))
    }

    @Test
    fun `a throwing awtCanBrowse probe degrades to the fallback instead of propagating`() {
        var command: List<String>? = null

        launcher(
            osName = "Linux",
            awtCanBrowse = { throw UnsatisfiedLinkError("no gtk") },
            exec = {
                command = it
                0
            },
        ).launch(URL)

        assertEquals(listOf("xdg-open", URL), command)
    }

    @Test
    fun `platformOpenCommand maps each supported OS family`() {
        assertEquals(listOf("open", URL), platformOpenCommand("Mac OS X", URL))
        assertEquals(
            listOf("rundll32", "url.dll,FileProtocolHandler", URL),
            platformOpenCommand("Windows 11", URL),
        )
        assertEquals(listOf("xdg-open", URL), platformOpenCommand("Linux", URL))
        assertNull(platformOpenCommand("SomeFutureOS", URL))
    }
}
