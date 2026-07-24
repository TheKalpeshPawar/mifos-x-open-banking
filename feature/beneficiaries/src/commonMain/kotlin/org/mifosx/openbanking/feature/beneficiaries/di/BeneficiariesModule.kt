/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesViewModel

/**
 * Koin bindings for the beneficiaries feature. Included by `cmp-navigation`'s feature module.
 *
 * The view model's `BeneficiariesRepository` is resolved from the wider graph (`BankingModule`);
 * Koin supplies the `SavedStateHandle` for the nav argument with no extra wiring.
 */
val BeneficiariesModule = module {
    viewModelOf(::BeneficiariesViewModel)
}
