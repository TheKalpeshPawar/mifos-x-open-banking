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

/**
 * Thrown by [BrowserLauncher.launch] when no browser could be opened at all.
 *
 * [BrowserLauncher.isSupported] is a *static* capability answer, decided before the consent request
 * is made. Whether a browser actually opens can only be known at the moment of launching, so this
 * covers the residual runtime failure — notably desktop Linux, where AWT's `Desktop.Action.BROWSE`
 * is frequently unavailable under Wayland and the `xdg-open` fallback may also be absent.
 *
 * Implementations MUST throw this rather than returning silently: the caller moves to
 * `LoginUiState.Authorising` on the assumption a browser opened, so a quiet no-op strands the PSU
 * on a spinner that no redirect will ever clear.
 */
class BrowserLaunchException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

interface BrowserLauncher {
    /**
     * Whether this platform can complete the whole OAuth round-trip — open the authorisation URL
     * *and* receive HSBC's redirect back into the app.
     *
     * Opening a browser is not sufficient on its own: without a redirect receiver the PSU authorises
     * at HSBC and the consent silently never completes. Platforms that cannot receive the redirect
     * report `false` so [org.mifosx.openbanking.feature.login.ui.LoginViewModel] can surface an
     * explicit unsupported error instead of stranding the user on a spinner.
     */
    val isSupported: Boolean
        get() = true

    /**
     * Opens [url] in the PSU's browser.
     *
     * @throws BrowserLaunchException if no browser could be opened. Never fail silently — see
     * [BrowserLaunchException].
     */
    fun launch(url: String)
}
