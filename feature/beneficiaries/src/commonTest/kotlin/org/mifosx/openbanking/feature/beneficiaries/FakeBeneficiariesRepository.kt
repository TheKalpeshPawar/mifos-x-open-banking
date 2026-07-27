/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.mifosx.openbanking.core.data.banking.BeneficiariesRepository
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import template.core.base.common.screen.ScreenState
import template.core.base.store.screen.ScreenDataStream

/**
 * Hand-written [BeneficiariesRepository] whose stream the test drives directly.
 *
 * Test source sets do not cross Gradle modules, so this is re-declared locally rather than imported
 * from `core:data`. The refresh trigger is replay-buffered because `refresh()` is a `tryEmit`: on a
 * bare `MutableSharedFlow` with no subscriber the emission is dropped and [refreshCount] silently
 * stays at zero.
 */
class FakeBeneficiariesRepository(
    initial: ScreenState<List<BeneficiaryItem>> = ScreenState.Loading,
) : BeneficiariesRepository {

    private val states = MutableStateFlow(initial)
    private val refreshes = MutableSharedFlow<Unit>(replay = REFRESH_REPLAY)

    var observedAccountId: String? = null
        private set

    val refreshCount: Int get() = refreshes.replayCache.size

    fun emit(state: ScreenState<List<BeneficiaryItem>>) {
        states.value = state
    }

    override fun beneficiariesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<BeneficiaryItem>> {
        observedAccountId = accountId
        return ScreenDataStream(state = states, refreshTrigger = refreshes)
    }

    private companion object {
        const val REFRESH_REPLAY = 16
    }
}
