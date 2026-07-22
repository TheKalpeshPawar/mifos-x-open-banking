/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifosx.openbanking.feature.statements.ui.StatementsViewModel

/**
 * Koin bindings for the statements feature. Included by `cmp-navigation`'s feature module.
 *
 * The view model's `StatementFileRepository` and `StatementFileHandler` dependencies are resolved
 * from the wider graph — the handler is bound in the app layer in a later slice, so runtime
 * resolution is incomplete until then; the module itself compiles and loads as-is.
 */
val StatementsModule = module {
    viewModelOf(::StatementsViewModel)
}
