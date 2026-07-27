/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login.di

import org.koin.dsl.module
import org.mifosx.openbanking.feature.login.browser.BrowserLauncher
import org.mifosx.openbanking.feature.login.browser.JsBrowserLauncher

/**
 * Lives in `jsCommonMain` so it satisfies the `expect` for BOTH the `js` and `wasmJs` targets — the
 * hierarchy template groups them under `jsCommon`. Previously this actual sat in `jsMain` only,
 * leaving `wasmJs` with no actual at all.
 */
actual val loginPlatformModule = module {
    single<BrowserLauncher> { JsBrowserLauncher() }
}
