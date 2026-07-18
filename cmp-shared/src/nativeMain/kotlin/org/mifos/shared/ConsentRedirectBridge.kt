/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifos.shared

import org.mifosx.openbanking.core.data.callback.ConsentRedirectBus

/**
 * Swift's entry point for HSBC's consent redirect.
 *
 * iOS delivers the callback to the SwiftUI layer via `onOpenURL`, which sits outside Kotlin
 * entirely, so `iOSApp.swift` hands the URL back across the ComposeApp framework boundary here.
 * Exposed to Swift as `ConsentRedirectBridgeKt.handleConsentRedirect(url:)`.
 *
 * Ignores anything that is not our callback scheme, since `onOpenURL` fires for every URL the app is
 * registered to open.
 */
fun handleConsentRedirect(url: String) {
    if (!url.startsWith("$CONSENT_REDIRECT_SCHEME://")) return
    ConsentRedirectBus.publish(url)
}

/** Must stay in lock-step with `CFBundleURLSchemes` in `cmp-ios/iosApp/Info.plist`. */
private const val CONSENT_REDIRECT_SCHEME = "org.mifosx.openbanking"
