/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.mifosx.openbanking.feature.transactions.ui.TransactionsViewModel

val TransactionsModule = module {
    viewModel { params ->
        TransactionsViewModel(
            transactionsRepository = get(),
            paymentsRepository = get(),
            accountsRepository = get(),
            bankId = params.get(0),
            accountId = params.get(1),
        )
    }
}
