/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.fxrates.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.fx.FxRepository
import org.mifosx.openbanking.core.model.obp.FxRate
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * Exchange Rates ViewModel — a live currency converter over the OBP per-pair FX endpoint
 * plus a fixed set of popular pairs. The selected pair's rate is mandatory (failure is a
 * screen-level error with retry); popular rows are supplementary and drop out individually
 * when their pair fails. Rates fetched once are kept for the session; switching currencies
 * fetches only pairs not seen yet, and pairs the bank cannot quote are remembered so the
 * converter shows an unavailable notice instead of refetching.
 */
class FxRatesViewModel(
    private val fxRepository: FxRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<ScreenState<FxRatesContent>> =
        combine(rawState, selection) { raw, sel -> project(raw, sel) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ScreenState.Loading,
            )

    init {
        load()
    }

    fun onRetry() = load()

    /** Accepts digits and at most one decimal point; everything else is dropped. */
    fun onAmountChanged(raw: String) {
        var dotSeen = false
        val sanitized = buildString {
            raw.forEach { ch ->
                when {
                    ch.isDigit() -> append(ch)
                    ch == '.' && !dotSeen -> {
                        dotSeen = true
                        append(ch)
                    }
                }
            }
        }.take(MAX_AMOUNT_LENGTH)
        selection.update { it.copy(amountInput = sanitized) }
    }

    /** Picking the currency already on the other side swaps the pair instead of duplicating it. */
    fun onFromCurrencySelected(code: String) {
        val sel = selection.value
        if (code == sel.toCurrency) {
            onSwapCurrencies()
        } else {
            selection.update { it.copy(fromCurrency = code) }
            ensureRate(code, sel.toCurrency)
        }
    }

    /** Picking the currency already on the other side swaps the pair instead of duplicating it. */
    fun onToCurrencySelected(code: String) {
        val sel = selection.value
        if (code == sel.fromCurrency) {
            onSwapCurrencies()
        } else {
            selection.update { it.copy(toCurrency = code) }
            ensureRate(sel.fromCurrency, code)
        }
    }

    fun onSwapCurrencies() {
        val sel = selection.value
        selection.update { it.copy(fromCurrency = sel.toCurrency, toCurrency = sel.fromCurrency) }
        ensureRate(sel.toCurrency, sel.fromCurrency)
    }

    fun onPairSelected(from: String, to: String) {
        selection.update { it.copy(fromCurrency = from, toCurrency = to) }
        ensureRate(from, to)
    }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            val sel = selection.value
            val selected = fxRepository.getRate(sel.fromCurrency, sel.toCurrency).getOrElse {
                rawState.value = RawState.Failed(it)
                return@launch
            }
            val currencies = fxRepository.supportedCurrencies()
            val popular = coroutineScope {
                POPULAR_PAIRS.map { (from, to) ->
                    async { rateKey(from, to) to fxRepository.getRate(from, to).getOrNull() }
                }.awaitAll()
            }
            val rates = buildMap {
                popular.forEach { (key, rate) -> if (rate != null) put(key, rate) }
                put(rateKey(sel.fromCurrency, sel.toCurrency), selected)
            }
            rawState.value = RawState.Loaded(currencies = currencies, rates = rates)
        }
    }

    /** Fetches the pair's rate unless it is already cached or known to be unquotable. */
    private fun ensureRate(from: String, to: String) {
        val loaded = rawState.value as? RawState.Loaded ?: return
        val key = rateKey(from, to)
        if (key in loaded.rates || key in loaded.unavailable) return
        viewModelScope.launch {
            val result = fxRepository.getRate(from, to)
            val current = rawState.value as? RawState.Loaded ?: return@launch
            rawState.value = result.fold(
                onSuccess = { current.copy(rates = current.rates + (key to it)) },
                onFailure = { current.copy(unavailable = current.unavailable + key) },
            )
        }
    }

    private fun project(raw: RawState, sel: Selection): ScreenState<FxRatesContent> = when (raw) {
        is RawState.Loading -> ScreenState.Loading
        is RawState.Failed -> ScreenState.Error(raw.error)
        is RawState.Loaded -> ScreenState.Content(
            data = loadedContent(raw, sel),
            freshness = DataFreshness.FRESH,
        )
    }

    private fun loadedContent(raw: RawState.Loaded, sel: Selection): FxRatesContent {
        val rate = raw.rates[rateKey(sel.fromCurrency, sel.toCurrency)]
        val amount = sel.amountInput.toDoubleOrNull() ?: 0.0
        val converted = rate?.let { formatFxAmount(amount * it.conversionValue) }
        return FxRatesContent(
            fromCurrency = sel.fromCurrency,
            toCurrency = sel.toCurrency,
            amountInput = sel.amountInput,
            currencies = (raw.currencies + sel.fromCurrency + sel.toCurrency).distinct(),
            rateAvailable = rate != null,
            convertedLabel = converted?.let { "= $it ${sel.toCurrency}" }.orEmpty(),
            rateLabel = rate?.let {
                "Rate: 1 ${sel.fromCurrency} = ${formatFxRate(it.conversionValue)} ${sel.toCurrency}"
            }.orEmpty(),
            lastUpdatedLabel = rate?.effectiveDate
                ?.takeIf { it.isNotBlank() }
                ?.let { "Rates updated: ${formatFxTimestamp(it)}" }
                .orEmpty(),
            pairs = POPULAR_PAIRS.mapNotNull { (from, to) ->
                raw.rates[rateKey(from, to)]?.let {
                    FxPairRow(fromCurrency = from, toCurrency = to, rateLabel = formatFxRate(it.conversionValue))
                }
            },
        )
    }

    private fun rateKey(from: String, to: String) = "$from/$to"

    private data class Selection(
        val fromCurrency: String = DEFAULT_FROM,
        val toCurrency: String = DEFAULT_TO,
        val amountInput: String = DEFAULT_AMOUNT,
    )

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(
            val currencies: List<String>,
            val rates: Map<String, FxRate>,
            val unavailable: Set<String> = emptySet(),
        ) : RawState
    }

    private companion object {
        const val DEFAULT_FROM = "GBP"
        const val DEFAULT_TO = "EUR"
        const val DEFAULT_AMOUNT = "1000"
        const val MAX_AMOUNT_LENGTH = 12
    }
}

/** Spec-defined popular pairs, all quotable on the sandbox FX engine (verified live). */
internal val POPULAR_PAIRS = listOf(
    "GBP" to "EUR",
    "GBP" to "USD",
    "GBP" to "JPY",
    "EUR" to "USD",
    "USD" to "INR",
    "EUR" to "GBP",
)

/** Loaded content for the Exchange Rates screen. */
@Immutable
data class FxRatesContent(
    val fromCurrency: String,
    val toCurrency: String,
    val amountInput: String,
    val currencies: List<String>,
    val rateAvailable: Boolean,
    val convertedLabel: String,
    val rateLabel: String,
    val lastUpdatedLabel: String,
    val pairs: List<FxPairRow>,
)

/** One popular-pair row ("GBP / EUR" with its formatted live rate). */
@Immutable
data class FxPairRow(
    val fromCurrency: String,
    val toCurrency: String,
    val rateLabel: String,
)

/** "1162.781" → "1,162.78" — grouped thousands, exactly two decimals. */
internal fun formatFxAmount(value: Double): String {
    val cents = kotlin.math.round(value * 100).toLong()
    val whole = (cents / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    val frac = (cents % 100).toString().padStart(2, '0')
    return "$whole.$frac"
}

/** Rate to at most four decimals with trailing zeros trimmed ("1.1628", "141.373", "0.86"). */
internal fun formatFxRate(value: Double): String {
    val scaled = kotlin.math.round(value * 10_000).toLong()
    val whole = scaled / 10_000
    val frac = (scaled % 10_000).toString().padStart(4, '0').trimEnd('0')
    return if (frac.isEmpty()) whole.toString() else "$whole.$frac"
}

/** "2026-06-07T11:44:10Z" → "7 June 2026, 11:44 UTC" (unparseable input passes through). */
internal fun formatFxTimestamp(iso: String): String {
    val parts = iso.take(10).split('-')
    val day = parts.getOrNull(2)?.toIntOrNull()
    val monthName = parts.getOrNull(1)?.toIntOrNull()?.let { FX_MONTHS.getOrNull(it - 1) }
    if (parts.size != 3 || day == null || monthName == null) return iso
    val time = iso.substringAfter('T', "").take(5)
    return if (time.isBlank()) "$day $monthName ${parts[0]}" else "$day $monthName ${parts[0]}, $time UTC"
}

private val FX_MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
