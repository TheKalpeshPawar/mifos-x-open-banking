/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailViewModel

/**
 * Koin bindings for the statement-detail feature. Included by `cmp-navigation`'s feature module.
 *
 * The view model's `StatementDetailRepository` and `StatementFileRepository` come from the data layer;
 * the `StatementFileHandler` is bound by the app layer (shared with `feature/statements`), so runtime
 * resolution of the handler depends on that binding being present.
 */
val StatementDetailModule = module {
    viewModelOf(::StatementDetailViewModel)
}
