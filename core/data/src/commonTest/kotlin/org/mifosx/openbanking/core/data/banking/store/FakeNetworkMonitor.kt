/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.store

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [NetworkMonitor], mirroring the framework fixture that lives in another module's
 * unreachable commonTest. Exposes a mutable [networkStatus] flow so tests can seed an online status
 * and let [asScreenStream] treat the device as connected.
 */
class FakeNetworkMonitor(initialStatus: NetworkStatus) : NetworkMonitor {

    private val mutableStatus = MutableStateFlow(initialStatus)

    override val networkStatus: StateFlow<NetworkStatus> = mutableStatus.asStateFlow()

    override val isOnline: StateFlow<Boolean> =
        MutableStateFlow(initialStatus is NetworkStatus.Available).asStateFlow()

    override val networkChanges: SharedFlow<NetworkChangeEvent> =
        MutableSharedFlow<NetworkChangeEvent>().asSharedFlow()

    fun setStatus(status: NetworkStatus) {
        mutableStatus.value = status
    }

    override fun close() = Unit
}
