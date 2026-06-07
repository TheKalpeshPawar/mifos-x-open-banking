/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsViewModel

val DirectDebitsModule = module {
    viewModel { params ->
        DirectDebitsViewModel(
            directDebitsRepository = get(),
            accountsRepository = get(),
            userPreferencesRepository = get(),
            bankId = params.get(0),
            accountId = params.get(1),
        )
    }
}
