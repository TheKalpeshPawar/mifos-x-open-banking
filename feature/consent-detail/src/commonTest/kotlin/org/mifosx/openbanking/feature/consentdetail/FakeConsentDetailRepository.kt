/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.mifosx.openbanking.core.data.banking.ConsentDetailRepository
import org.mifosx.openbanking.core.model.banking.ConsentSummary
import template.core.base.common.screen.ScreenState
import template.core.base.store.screen.ScreenDataStream

private const val REFRESH_REPLAY = 16

/**
 * Hand-written [ConsentDetailRepository] whose stream the test drives directly.
 *
 * Test source sets do not cross Gradle modules, so this is re-declared locally. The refresh trigger
 * is replay-buffered because `refresh()` is a `tryEmit`: on a bare `MutableSharedFlow` with no
 * subscriber the emission is dropped and [refreshCount] silently stays at zero.
 */
class FakeConsentDetailRepository(
    initial: ScreenState<ConsentSummary> = ScreenState.Loading,
) : ConsentDetailRepository {

    private val states = MutableStateFlow(initial)
    private val refreshes = MutableSharedFlow<Unit>(replay = REFRESH_REPLAY)

    var observedConsentId: String? = null
        private set

    val refreshCount: Int get() = refreshes.replayCache.size

    fun emit(state: ScreenState<ConsentSummary>) {
        states.value = state
    }

    override fun consentStream(
        consentId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<ConsentSummary> {
        observedConsentId = consentId
        return ScreenDataStream(state = states, refreshTrigger = refreshes)
    }
}
