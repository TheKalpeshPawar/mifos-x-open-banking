/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mifosx.openbanking.feature.sendmoney.ui.ScaChallengeViewModel
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyConfirmViewModel
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyHubViewModel
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyViewModel

val SendMoneyModule = module {
    viewModelOf(::SendMoneyHubViewModel)
    viewModel {
        SendMoneyViewModel(
            accountsRepository = get(),
            paymentsRepository = get(),
            banksRepository = get(),
            isSandbox = get(named("isSandbox")),
        )
    }
    viewModelOf(::SendMoneyConfirmViewModel)
    viewModel { params ->
        ScaChallengeViewModel(
            paymentsRepository = get(),
            bankId = params.get(0),
            accountId = params.get(1),
            type = params.get(2),
            requestId = params.get(3),
            challengeId = params.get(4),
        )
    }
}
