/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.businessinsights.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.mifosx.openbanking.feature.businessinsights.ui.BusinessInsightsViewModel

val BusinessInsightsModule = module {
    viewModel {
        BusinessInsightsViewModel(
            pfmAccountsService = get(),
            transactionsRepository = get(),
        )
    }
}
