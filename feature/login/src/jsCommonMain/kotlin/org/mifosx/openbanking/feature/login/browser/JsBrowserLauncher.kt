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
 * Web (js / wasmJs) has no consent-redirect receiver wired, so the OAuth round-trip cannot complete
 * on this platform. Reports [isSupported] = `false` so the login screen refuses up front with an
 * explicit message rather than opening HSBC and stranding the PSU on a callback that never arrives.
 */
class JsBrowserLauncher : BrowserLauncher {
    override val isSupported: Boolean = false

    /**
     * Intentionally does nothing: [isSupported] is `false`, so `LoginViewModel` errors out before
     * this is ever reached. Kept total rather than throwing so an accidental call cannot crash the
     * app on a platform the consent journey simply does not serve.
     */
    override fun launch(url: String) = Unit
}
