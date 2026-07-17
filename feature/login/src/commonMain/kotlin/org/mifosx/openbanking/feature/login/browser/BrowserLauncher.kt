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

    fun launch(url: String)
}
