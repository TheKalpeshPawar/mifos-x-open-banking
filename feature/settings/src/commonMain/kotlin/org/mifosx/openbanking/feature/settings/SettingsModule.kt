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

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mifosx.openbanking.feature.settings.ui.SettingsViewModel

val SettingsModule = module {
    viewModel {
        SettingsViewModel(
            userDataRepository = get(),
            profileRepository = get(),
            authRecoveryRepository = get(),
            obpAuthRepository = get(),
            appVersion = get<String>(named("appVersion")).ifBlank { "v1.0.0" },
        )
    }
}
