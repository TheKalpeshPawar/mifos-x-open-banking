/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentlist.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListViewModel

/**
 * Koin bindings for the consent-list feature. Included by `cmp-navigation`'s feature module.
 *
 * The view model's `ConsentsRepository` and `ConsentSession` are both resolved from the wider graph
 * (`BankingModule` and `RepositoryModule` respectively).
 */
val ConsentListModule = module {
    viewModelOf(::ConsentListViewModel)
}
