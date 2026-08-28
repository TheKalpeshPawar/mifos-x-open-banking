/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package cmp.navigation.authenticatednavhost

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import template.core.base.ui.viewmodel.BaseViewModel

/** Exposes the connectivity state that drives the host's offline snackbar. */
internal class AuthenticatedNavHostViewModel(
    networkMonitor: NetworkMonitor,
) : BaseViewModel<Unit, Nothing, AuthenticatedNavHostAction>(
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
    override fun handleAction(action: AuthenticatedNavHostAction) {
    }
}

internal sealed interface AuthenticatedNavHostAction
