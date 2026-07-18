/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package cmp.navigation.rootnav

import androidx.lifecycle.viewModelScope
import cmp.navigation.rootnav.RootNavAction.Internal.UserStateUpdateReceive
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.mifosx.openbanking.core.data.callback.ConsentSession
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.model.user.UserData
import template.core.base.ui.viewmodel.BaseViewModel

class RootNavViewModel(
    userDataRepository: UserDataRepository,
    private val consentSession: ConsentSession,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState.Splash,
) {

    init {
        userDataRepository.userData
            .map(::UserStateUpdateReceive)
            .onEach(::handleAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: RootNavAction) {
        when (action) {
            is UserStateUpdateReceive -> handleUserStateUpdateReceive(action)
        }
    }

    /**
     * Signed-in means an authorised consent produced PSU tokens — [ConsentSession] — not a stored
     * flag. `UserData.isAuthenticated` cannot carry that: it is only ever written `false`, by
     * `clearUserData()` on logout, and nothing sets it `true`. It therefore never rises above its
     * default, which is why a default of `true` sent every launch straight past login to Home.
     *
     * There is no passcode in this app, so no lock gate: the old `passcode.isEmpty()` branch routed
     * to a `UserLocked` state that had no destination at all, which is why the default carried a
     * hardcoded "1234" to avoid ever reaching it.
     */
    private fun handleUserStateUpdateReceive(
        action: UserStateUpdateReceive,
    ) {
        val userData = action.userData

        val updatedRootNavState = when {
            !consentSession.isActive() -> RootNavState.Auth

            else -> RootNavState.UserUnlocked(userData.activeUserId)
        }

        mutableStateFlow.update { updatedRootNavState }
    }
}

sealed class RootNavState {
    data object Auth : RootNavState()

    data object Splash : RootNavState()

    data class UserUnlocked(
        val activeUserId: String,
    ) : RootNavState()
}

sealed class RootNavAction {

    sealed class Internal {

        data class UserStateUpdateReceive(
            val userData: UserData,
        ) : RootNavAction()
    }
}
