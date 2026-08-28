/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package template.core.base.common.screen

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

/**
 * Transforms only [ScreenState.Content] data, passing through all other states unchanged.
 */
fun <T, R> Flow<ScreenState<T>>.mapContent(
    transform: (data: T, freshness: DataFreshness) -> R,
): Flow<ScreenState<R>> = map { state ->
    when (state) {
        is ScreenState.Content -> ScreenState.Content(
            data = transform(state.data, state.freshness),
            freshness = state.freshness,
            fetchedAt = state.fetchedAt,
        )
        is ScreenState.Loading -> ScreenState.Loading
        is ScreenState.Empty -> ScreenState.Empty
        is ScreenState.NoNetwork -> state
        is ScreenState.Error -> state
        is ScreenState.Unauthenticated -> state
    }
}

/**
 * Combines ScreenState with a local state flow for reactive filter/sort/preferences.
 */
fun <T, S, R> Flow<ScreenState<T>>.combineContent(
    other: Flow<S>,
    transform: (data: T, extra: S, freshness: DataFreshness) -> R,
): Flow<ScreenState<R>> = combine(this, other) { state, extra ->
    when (state) {
        is ScreenState.Content -> ScreenState.Content(
            data = transform(state.data, extra, state.freshness),
            freshness = state.freshness,
            fetchedAt = state.fetchedAt,
        )
        is ScreenState.Loading -> ScreenState.Loading
        is ScreenState.Empty -> ScreenState.Empty
        is ScreenState.NoNetwork -> state
        is ScreenState.Error -> state
        is ScreenState.Unauthenticated -> state
    }
}

/**
 * Converts Content to Empty when business-level predicate says data is empty.
 * Applied AFTER DecisionEngine (which only handles structural empty from Store).
 */
fun <T> Flow<ScreenState<T>>.emptyIfContent(
    predicate: (T) -> Boolean,
): Flow<ScreenState<T>> = map { state ->
    when (state) {
        is ScreenState.Content -> if (predicate(state.data)) ScreenState.Empty else state
        else -> state
    }
}

/** Extracts data from Content state or null for other states. */
val <T> ScreenState<T>.dataOrNull: T?
    get() = (this as? ScreenState.Content)?.data

/** True if state has displayable content. */
val <T> ScreenState<T>.hasContent: Boolean
    get() = this is ScreenState.Content

/** Maps Error throwable to a user-facing type. */
fun <T> Flow<ScreenState<T>>.mapError(
    transform: (Throwable) -> Throwable,
): Flow<ScreenState<T>> = map { state ->
    when (state) {
        is ScreenState.Error -> ScreenState.Error(
            error = transform(state.error),
            isNetworkError = state.isNetworkError,
        )
        else -> state
    }
}

/**
 * Refresh helper for manually-managed screen state (non-Store5): keep showing data as
 * [ScreenState.Content] with [DataFreshness.UPDATING] when present, else [ScreenState.Loading].
 *
 * Replacement for the removed `MutableStateFlow<DataState<T>>.updateToPendingOrLoading`.
 */
fun <T> MutableStateFlow<ScreenState<T>>.updateToUpdatingOrLoading() {
    update { state ->
        state.dataOrNull
            ?.let { ScreenState.Content(it, DataFreshness.UPDATING) }
            ?: ScreenState.Loading
    }
}

/**
 * Wraps any value in a [ScreenState.Content] with [DataFreshness.FRESH].
 * Use for utility or settings screens that hold local state and don't use Store5.
 */
fun <T> T.asLocalScreenState(): ScreenState<T> =
    ScreenState.Content(this, DataFreshness.FRESH)

/**
 * Turns any [Flow]<T> into a [Flow]<[ScreenState]<T>>.
 *
 * - Emits [ScreenState.Loading] before the first value
 * - Maps each value to [ScreenState.Content] with [DataFreshness.FRESH]
 * - Catches exceptions → [ScreenState.Error]
 *
 * Use on settings, calculator, or other screens that have no network source.
 * For network-backed screens use `Store.asScreenStream` instead.
 */
fun <T> Flow<T>.asLocalScreenStream(): Flow<ScreenState<T>> =
    map { it.asLocalScreenState() }
        .onStart { emit(ScreenState.Loading) }
        .catch { emit(ScreenState.Error(it)) }

/**
 * Combines two independent [ScreenState] flows into one, merging their [ScreenState.Content]
 * data via [transform] when both sources have loaded.
 *
 * **Priority** (first match wins on each emission):
 * `NoNetwork > Loading > Unauthenticated > Error > Empty > Content`
 *
 * The resulting [DataFreshness] is the most severe of the sources, by declaration order:
 * `STALE_OFFLINE > STALE_FAILED > UPDATING > FRESH`.
 */
@OptIn(ExperimentalTime::class)
fun <A, B, R> combineScreenStates(
    a: Flow<ScreenState<A>>,
    b: Flow<ScreenState<B>>,
    transform: (A, B) -> R,
): Flow<ScreenState<R>> = combine(a, b) { sa, sb ->
    @Suppress("UNCHECKED_CAST")
    resolveScreenStates(listOf(sa, sb)) {
        transform(
            (sa as ScreenState.Content<A>).data,
            (sb as ScreenState.Content<B>).data,
        )
    }
}

/**
 * Combines three independent [ScreenState] flows. See [combineScreenStates] for priority rules.
 */
@OptIn(ExperimentalTime::class)
fun <A, B, C, R> combineScreenStates(
    a: Flow<ScreenState<A>>,
    b: Flow<ScreenState<B>>,
    c: Flow<ScreenState<C>>,
    transform: (A, B, C) -> R,
): Flow<ScreenState<R>> = combine(a, b, c) { sa, sb, sc ->
    @Suppress("UNCHECKED_CAST")
    resolveScreenStates(listOf(sa, sb, sc)) {
        transform(
            (sa as ScreenState.Content<A>).data,
            (sb as ScreenState.Content<B>).data,
            (sc as ScreenState.Content<C>).data,
        )
    }
}

/**
 * Combines four independent [ScreenState] flows. See [combineScreenStates] for priority rules.
 */
@OptIn(ExperimentalTime::class)
fun <A, B, C, D, R> combineScreenStates(
    a: Flow<ScreenState<A>>,
    b: Flow<ScreenState<B>>,
    c: Flow<ScreenState<C>>,
    d: Flow<ScreenState<D>>,
    transform: (A, B, C, D) -> R,
): Flow<ScreenState<R>> = combine(a, b, c, d) { sa, sb, sc, sd ->
    @Suppress("UNCHECKED_CAST")
    resolveScreenStates(listOf(sa, sb, sc, sd)) {
        transform(
            (sa as ScreenState.Content<A>).data,
            (sb as ScreenState.Content<B>).data,
            (sc as ScreenState.Content<C>).data,
            (sd as ScreenState.Content<D>).data,
        )
    }
}

/**
 * Combines five independent [ScreenState] flows. See [combineScreenStates] for priority rules.
 */
@OptIn(ExperimentalTime::class)
fun <A, B, C, D, E, R> combineScreenStates(
    a: Flow<ScreenState<A>>,
    b: Flow<ScreenState<B>>,
    c: Flow<ScreenState<C>>,
    d: Flow<ScreenState<D>>,
    e: Flow<ScreenState<E>>,
    transform: (A, B, C, D, E) -> R,
): Flow<ScreenState<R>> = combine(a, b, c, d, e) { sa, sb, sc, sd, se ->
    @Suppress("UNCHECKED_CAST")
    resolveScreenStates(listOf(sa, sb, sc, sd, se)) {
        transform(
            (sa as ScreenState.Content<A>).data,
            (sb as ScreenState.Content<B>).data,
            (sc as ScreenState.Content<C>).data,
            (sd as ScreenState.Content<D>).data,
            (se as ScreenState.Content<E>).data,
        )
    }
}

/**
 * Resolves N [ScreenState]s to a single result, applying the standard priority rule.
 *
 * Priority: NoNetwork > Loading > Unauthenticated > Error > Empty > Content
 * Freshness: worst across all Content sources, taken as the maximum over [DataFreshness]'s
 * declaration order (STALE_OFFLINE > STALE_FAILED > UPDATING > FRESH).
 * fetchedAt: null if any Content source has null fetchedAt; otherwise the minimum (oldest).
 */
@OptIn(ExperimentalTime::class)
private fun <R> resolveScreenStates(
    states: List<ScreenState<*>>,
    buildResult: () -> R,
): ScreenState<R> {
    val contents = states.filterIsInstance<ScreenState.Content<*>>()
    val worstFreshness = contents.fold(DataFreshness.FRESH) { acc, c -> maxOf(acc, c.freshness) }
    val combinedFetchedAt: Instant? = if (contents.any { it.fetchedAt == null }) {
        null
    } else {
        contents.mapNotNull { it.fetchedAt }.minOrNull()
    }

    return when {
        states.any { it is ScreenState.NoNetwork } -> ScreenState.NoNetwork()
        states.any { it is ScreenState.Loading } -> ScreenState.Loading
        states.any { it is ScreenState.Unauthenticated } -> ScreenState.Unauthenticated
        states.any { it is ScreenState.Error } -> {
            val err = states.first { it is ScreenState.Error } as ScreenState.Error
            ScreenState.Error(err.error, err.isNetworkError)
        }
        states.any { it is ScreenState.Empty } -> ScreenState.Empty
        states.size == contents.size ->
            ScreenState.Content(buildResult(), worstFreshness, combinedFetchedAt)
        else -> ScreenState.Loading
    }
}
