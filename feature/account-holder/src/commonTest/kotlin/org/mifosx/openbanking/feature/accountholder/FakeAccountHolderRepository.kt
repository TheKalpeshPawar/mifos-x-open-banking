/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.mifosx.openbanking.core.data.banking.ProfileRepository
import org.mifosx.openbanking.core.model.banking.PartyProfile
import template.core.base.common.screen.ScreenState
import template.core.base.store.screen.ScreenDataStream

/**
 * Hand-written [ProfileRepository] whose stream the test drives directly.
 *
 * Written by hand rather than mocked so the test states exactly which [ScreenState] the view model
 * sees and when. [observedAccountId] and [refreshCount] record what the view model asked for.
 */
class FakeAccountHolderRepository(
    initial: ScreenState<PartyProfile> = ScreenState.Loading,
) : ProfileRepository {

    private val states = MutableStateFlow(initial)
    private val refreshes = MutableSharedFlow<Unit>(replay = REFRESH_REPLAY)

    /** The account id the view model subscribed with; null until [profileStream] is called. */
    var observedAccountId: String? = null
        private set

    /** How many times the stream was refreshed — the Retry action's only observable effect. */
    val refreshCount: Int get() = refreshes.replayCache.size

    /** Pushes a new state to every active collector. */
    fun emit(state: ScreenState<PartyProfile>) {
        states.value = state
    }

    override fun profileStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<PartyProfile> {
        observedAccountId = accountId
        return ScreenDataStream(state = states, refreshTrigger = refreshes)
    }

    private companion object {
        /**
         * `refresh()` is a `tryEmit`, so a replay-less shared flow would drop every refresh with no
         * collector attached and leave [refreshCount] permanently at zero.
         */
        const val REFRESH_REPLAY = 16
    }
}
