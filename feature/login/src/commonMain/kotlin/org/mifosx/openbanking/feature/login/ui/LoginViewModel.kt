/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login.ui

import template.core.base.ui.viewmodel.BaseViewModel

/**
 * Login shell ViewModel. OBP `direct_login` token flow + credential validation
 * are wired in Phase 4 (feature wave A) per PLAN-kmp-greenfield-implementation.
 */
class LoginViewModel : BaseViewModel<Unit, Nothing, LoginAction>(initialState = Unit) {

    @Suppress("EmptyFunctionBlock")
    override fun handleAction(action: LoginAction) {
    }
}

sealed interface LoginAction
