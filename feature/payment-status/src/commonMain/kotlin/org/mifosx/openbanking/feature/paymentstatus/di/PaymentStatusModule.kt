/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusViewModel

/**
 * Koin bindings for the payment-status feature. Included by `cmp-navigation`'s feature module.
 *
 * Constructed explicitly rather than with `viewModelOf`, because the ViewModel's `clock` and
 * `timeZone` are defaulted constructor parameters and `viewModelOf` does not honour Kotlin defaults
 * — it would try to resolve a `Clock` from the graph and fail at runtime. They exist to be
 * overridden in tests, not to be injected in production.
 */
val PaymentStatusModule = module {
    viewModel {
        PaymentStatusViewModel(
            savedStateHandle = get(),
            repository = get(),
            paymentHistoryRepository = get(),
        )
    }
}
