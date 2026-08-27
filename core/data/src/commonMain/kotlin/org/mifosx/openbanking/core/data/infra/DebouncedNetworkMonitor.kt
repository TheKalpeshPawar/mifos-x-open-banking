/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.infra

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A [NetworkMonitor] whose connectivity signals are debounced and mutually consistent.
 *
 * [isOnline] is derived from the debounced [networkStatus], so the two can never report different
 * connectivity. Both are hot and seeded with the delegate's current value, so a collector sees a
 * value immediately.
 *
 * @param delegate The platform monitor supplying the raw signals.
 * @param scope App-lifetime scope the two state flows are shared in.
 * @param window How long connectivity must settle before a change is published.
 */
@OptIn(FlowPreview::class)
internal class DebouncedNetworkMonitor(
    private val delegate: NetworkMonitor,
    scope: CoroutineScope,
    window: Duration = DEFAULT_WINDOW,
) : NetworkMonitor by delegate {

    override val networkStatus: StateFlow<NetworkStatus> = delegate.networkStatus
        .debounce(window)
        .stateIn(scope, SharingStarted.Eagerly, delegate.networkStatus.value)

    override val isOnline: StateFlow<Boolean> = networkStatus
        .map { it is NetworkStatus.Available }
        .stateIn(scope, SharingStarted.Eagerly, delegate.isOnline.value)

    /**
     * Overridden because Kotlin resolves an interface's default member against the delegate, which
     * would return the raw status rather than the debounced one.
     */
    override val currentStatus: NetworkStatus get() = networkStatus.value

    private companion object {
        val DEFAULT_WINDOW: Duration = 300.milliseconds
    }
}
