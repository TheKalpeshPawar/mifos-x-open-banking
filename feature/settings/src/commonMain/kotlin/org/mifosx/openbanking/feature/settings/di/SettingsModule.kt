/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.settings.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifosx.openbanking.feature.settings.ui.SettingsAppVersion
import org.mifosx.openbanking.feature.settings.ui.SettingsViewModel

/** The build identity the App Version row shows. */
private const val APP_VERSION_LABEL = "0.1.0 (build 1)"

/** Koin bindings for the settings feature. Included by `cmp-navigation`'s feature module. */
val SettingsModule = module {
    single { SettingsAppVersion(APP_VERSION_LABEL) }
    viewModelOf(::SettingsViewModel)
}
