/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * Converts a [NetworkResult] into a [ScreenState] for plain-Ktor (non-Store5) repositories.
 *
 * - [NetworkResult.Success] -> [ScreenState.Empty] when [isEmpty] holds, else
 *   [ScreenState.Content] with [DataFreshness.FRESH].
 * - [NetworkResult.Error] -> [ScreenState.Unauthenticated] (401/403), [ScreenState.NoNetwork]
 *   (timeout/connectivity), or [ScreenState.Error] with the mapped throwable.
 */
fun <T> NetworkResult<T, *>.toScreenState(
    isEmpty: (T) -> Boolean = { false },
): ScreenState<T> = when (this) {
    is NetworkResult.Success ->
        if (isEmpty(data)) ScreenState.Empty else ScreenState.Content(data, DataFreshness.FRESH)

    is NetworkResult.Error -> when (error) {
        NetworkError.UNAUTHORIZED -> ScreenState.Unauthenticated
        NetworkError.REQUEST_TIMEOUT -> ScreenState.NoNetwork()
        else -> ScreenState.Error(error.toThrowable(), isNetworkError = false)
    }
}

/**
 * Turns a stream of [NetworkResult] into a [ScreenState] stream: emits [ScreenState.Loading] first,
 * then maps each result via [toScreenState]. The ScreenState replacement for the removed
 * `Flow<NetworkResult<D, *>>.asDataStateFlow()`.
 */
fun <T> Flow<NetworkResult<T, *>>.asScreenStateStream(
    isEmpty: (T) -> Boolean = { false },
): Flow<ScreenState<T>> =
    map { it.toScreenState(isEmpty) }
        .onStart { emit(ScreenState.Loading) }
