/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.settings

expect fun getPlatform(): Platform

enum class Platform {
    Android,
    Desktop,
    IOS,
    JS,
    Wasm,
}

expect fun supportsDynamicTheming(): Boolean

/**
 * Whether the current platform exposes a biometric authentication path. Drives whether the
 * Settings "Biometric Login" toggle is enabled. This is a coarse platform-capability signal;
 * precise hardware enrollment state (BiometricManager.canAuthenticate / LAContext) requires a
 * platform context and is deferred until biometric login is wired end-to-end.
 */
expect fun isBiometricAvailableOnDevice(): Boolean
