/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.pfm.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.mifosx.openbanking.feature.pfm.settings.PfmSettingsViewModel
import org.mifosx.openbanking.feature.pfm.ui.PfmDashboardViewModel

val PfmModule = module {
    viewModel {
        PfmDashboardViewModel(
            transactionsRepository = get(),
            pfmAccountsService = get(),
            fxConverter = get(),
            budgetsRepository = get(),
            userPreferencesRepository = get(),
        )
    }
    viewModel {
        PfmSettingsViewModel(
            pfmAccountsService = get(),
            budgetsRepository = get(),
            userPreferencesRepository = get(),
        )
    }
}
