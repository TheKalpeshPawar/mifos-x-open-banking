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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.fx.FxRepository
import org.mifosx.openbanking.core.model.obp.FxRate
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val SANDBOX_RATES = mapOf(
    ("GBP" to "EUR") to 1.16278,
    ("EUR" to "GBP") to 0.860011,
    ("GBP" to "USD") to 1.2493,
    ("GBP" to "JPY") to 141.373,
    ("EUR" to "USD") to 1.07428,
    ("USD" to "INR") to 67.3135,
)

private class FxRatesFakeRepository(
    var rates: Map<Pair<String, String>, Double> = SANDBOX_RATES,
    var currencies: List<String> = listOf("GBP", "EUR", "USD", "JPY", "INR", "AUD", "AED"),
) : FxRepository {
    var rateCalls = 0
    override suspend fun getRate(from: String, to: String): Result<FxRate> {
        rateCalls++
        return rates[from to to]
            ?.let {
                Result.success(
                    FxRate(
                        fromCurrencyCode = from,
                        toCurrencyCode = to,
                        conversionValue = it,
                        effectiveDate = "2026-06-07T11:44:10Z",
                    ),
                )
            }
            ?: Result.failure(IllegalStateException("no rate $from->$to"))
    }

    override suspend fun supportedCurrencies(): List<String> = currencies
}

class FxRatesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(repository: FxRatesFakeRepository = FxRatesFakeRepository()) =
        FxRatesViewModel(fxRepository = repository)

    private suspend fun TestScope.content(model: FxRatesViewModel): FxRatesContent {
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content, "expected Content, was $s")
        return s.data
    }

    @Test
    fun load_populatesConverterAndPopularPairs() = runTest(dispatcher) {
        val c = content(vm())
        assertEquals("GBP", c.fromCurrency)
        assertEquals("EUR", c.toCurrency)
        assertEquals("1000", c.amountInput)
        assertTrue(c.rateAvailable)
        assertEquals("= 1,162.78 EUR", c.convertedLabel)
        assertEquals("Rate: 1 GBP = 1.1628 EUR", c.rateLabel)
        assertEquals("Rates updated: 7 June 2026, 11:44 UTC", c.lastUpdatedLabel)
        assertEquals(6, c.pairs.size)
        assertEquals("1.1628", c.pairs.first().rateLabel)
        assertTrue(c.currencies.contains("AUD"))
    }

    @Test
    fun amountChange_recomputesConversion() = runTest(dispatcher) {
        val model = vm()
        content(model)
        model.onAmountChanged("250")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals("= 290.70 EUR", c.convertedLabel)
    }

    @Test
    fun amountInput_keepsDigitsAndSingleDecimalPoint() = runTest(dispatcher) {
        val model = vm()
        content(model)
        model.onAmountChanged("1,2a3.4.5")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals("123.45", c.amountInput)
    }

    @Test
    fun swap_usesInverseRate() = runTest(dispatcher) {
        val model = vm()
        content(model)
        model.onSwapCurrencies()
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals("EUR", c.fromCurrency)
        assertEquals("GBP", c.toCurrency)
        assertEquals("= 860.01 GBP", c.convertedLabel)
    }

    @Test
    fun pairSelection_populatesConverterWithoutRefetch() = runTest(dispatcher) {
        val repository = FxRatesFakeRepository()
        val model = vm(repository)
        content(model)
        val callsAfterLoad = repository.rateCalls
        model.onPairSelected("USD", "INR")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals("USD", c.fromCurrency)
        assertEquals("INR", c.toCurrency)
        assertEquals("Rate: 1 USD = 67.3135 INR", c.rateLabel)
        assertEquals(callsAfterLoad, repository.rateCalls)
    }

    @Test
    fun currencyChange_fetchesMissingRate() = runTest(dispatcher) {
        val repository = FxRatesFakeRepository(
            rates = SANDBOX_RATES + (("GBP" to "AUD") to 1.63992),
        )
        val model = vm(repository)
        content(model)
        model.onToCurrencySelected("AUD")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals("= 1,639.92 AUD", c.convertedLabel)
    }

    @Test
    fun unsupportedPair_flagsRateUnavailable() = runTest(dispatcher) {
        val model = vm()
        content(model)
        model.onToCurrencySelected("AED")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertFalse(c.rateAvailable)
    }

    @Test
    fun selectingSameCurrencyOnBothSidesSwapsInstead() = runTest(dispatcher) {
        val model = vm()
        content(model)
        model.onFromCurrencySelected("EUR")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals("EUR", c.fromCurrency)
        assertEquals("GBP", c.toCurrency)
    }

    @Test
    fun popularPairFailure_dropsRowOnly() = runTest(dispatcher) {
        val repository = FxRatesFakeRepository(rates = SANDBOX_RATES - ("GBP" to "JPY"))
        val c = content(vm(repository))
        assertEquals(5, c.pairs.size)
        assertTrue(c.pairs.none { it.fromCurrency == "GBP" && it.toCurrency == "JPY" })
    }

    @Test
    fun selectedRateFailure_isErrorAndRetryRecovers() = runTest(dispatcher) {
        val repository = FxRatesFakeRepository(rates = emptyMap())
        val model = vm(repository)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)

        repository.rates = SANDBOX_RATES
        model.onRetry()
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Content)
    }
}
