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

import java.awt.Desktop
import java.net.URI
import java.util.Locale

/**
 * Resolves the OS-native URL opener, or `null` when [osName] is not one we know how to open on.
 *
 * Kept separate from [DesktopBrowserLauncher] so the mapping is verifiable without a real desktop.
 */
internal fun platformOpenCommand(osName: String, url: String): List<String>? {
    val os = osName.lowercase(Locale.ROOT)
    return when {
        os.contains("mac") || os.contains("darwin") -> listOf("open", url)
        os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
        os.contains("nix") || os.contains("nux") || os.contains("aix") -> listOf("xdg-open", url)
        else -> null
    }
}

/**
 * Opens the HSBC authorisation URL in the PSU's default browser.
 *
 * [isSupported] stays `true`: desktop runs `ConsentRedirectListener` on loopback, so it genuinely
 * completes the OAuth round-trip.
 *
 * AWT alone is not enough to open the browser. `Desktop.isDesktopSupported()` only reports that a
 * [Desktop] instance exists — it says nothing about any individual action, and on Linux under
 * Wayland `Desktop.Action.BROWSE` is routinely unsupported because the JDK cannot resolve
 * `xdg-open` through libgtk. Guarding on the wrong predicate meant `browse()` threw
 * `UnsupportedOperationException` on the AWT event thread, killing the login coroutine and leaving
 * the PSU on a dead button. So: check the *action*, then fall back to the platform opener.
 */
class DesktopBrowserLauncher internal constructor(
    private val osName: String,
    private val awtBrowse: (URI) -> Unit,
    private val awtCanBrowse: () -> Boolean,
    private val exec: (List<String>) -> Int,
) : BrowserLauncher {

    constructor() : this(
        osName = System.getProperty("os.name").orEmpty(),
        awtBrowse = { Desktop.getDesktop().browse(it) },
        awtCanBrowse = {
            Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
        },
        exec = { command ->
            // Wait for the exit code: ProcessBuilder.start() succeeds even when the opener goes on
            // to fail, and treating that as success would resurrect the silent no-op this class
            // exists to prevent.
            ProcessBuilder(command).start().waitFor()
        },
    )

    override fun launch(url: String) {
        val failures = mutableListOf<String>()

        if (tryAwt(url, failures)) return
        if (tryPlatformCommand(url, failures)) return

        throw BrowserLaunchException(
            "Could not open a browser for the HSBC authorisation URL. Attempts: " +
                failures.joinToString("; "),
        )
    }

    private fun tryAwt(url: String, failures: MutableList<String>): Boolean {
        if (!runCatching { awtCanBrowse() }.getOrDefault(false)) {
            failures += "AWT BROWSE unsupported"
            return false
        }
        return try {
            awtBrowse(URI(url))
            true
        } catch (e: Exception) {
            failures += "AWT browse failed (${e.message})"
            false
        }
    }

    private fun tryPlatformCommand(url: String, failures: MutableList<String>): Boolean {
        val command = platformOpenCommand(osName, url)
        if (command == null) {
            failures += "no known opener for OS '$osName'"
            return false
        }
        return try {
            val exit = exec(command)
            if (exit == 0) {
                true
            } else {
                failures += "${command.first()} exited $exit"
                false
            }
        } catch (e: Exception) {
            failures += "${command.first()} failed (${e.message})"
            false
        }
    }
}
