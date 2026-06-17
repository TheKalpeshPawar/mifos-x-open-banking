/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.authenticatednavbar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import template.core.base.ui.viewmodel.BaseViewModel

/**
 * Navbar shell ViewModel. Bottom-nav navigation is handled directly in the
 * Compose layer against the active flavor's tab set; this VM only exposes the
 * connectivity state that drives the offline snackbar.
 */
internal class AuthenticatedNavbarNavigationViewModel(
    networkMonitor: NetworkMonitor,
) : BaseViewModel<Unit, Nothing, AuthenticatedNavBarAction>(
    initialState = Unit,
) {

    val isOffline = networkMonitor.isOnline
        .map(Boolean::not)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    @Suppress("EmptyFunctionBlock")
    override fun handleAction(action: AuthenticatedNavBarAction) {
    }
}

internal sealed interface AuthenticatedNavBarAction
