/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.mifosx.openbanking.feature.home.ui.HomeViewModel

val HomeModule = module {
    viewModel {
        HomeViewModel(
            accountsRepository = get(),
        )
    }
}
