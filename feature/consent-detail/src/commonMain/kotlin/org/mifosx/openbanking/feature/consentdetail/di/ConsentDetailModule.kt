/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailViewModel

/**
 * Koin bindings for the consent-detail feature. Included by `cmp-navigation`'s feature module.
 *
 * The view model's `ConsentDetailRepository`, `ConsentRevokeRepository` and `ConsentSession` are all
 * resolved from the wider graph; Koin supplies the `SavedStateHandle` for the nav argument with no
 * extra wiring.
 */
val ConsentDetailModule = module {
    viewModelOf(::ConsentDetailViewModel)
}
