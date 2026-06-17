/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.fx

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.model.obp.BankCurrency
import org.mifosx.openbanking.core.model.obp.CurrenciesResponse
import org.mifosx.openbanking.core.model.obp.FxRate
import org.mifosx.openbanking.core.network.api.FxApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FxRepositoryTest {

    private class FakeFxApi(
        var rate: NetworkResult<FxRate, NetworkError> =
            NetworkResult.Success(FxRate(conversionValue = 1.16278)),
        var currencies: NetworkResult<CurrenciesResponse, NetworkError> =
            NetworkResult.Success(CurrenciesResponse()),
    ) : FxApi {
        override suspend fun getRate(
            bankId: String,
            fromCurrencyCode: String,
            toCurrencyCode: String,
        ): NetworkResult<FxRate, NetworkError> = rate

        override suspend fun getCurrencies(bankId: String): NetworkResult<CurrenciesResponse, NetworkError> =
            currencies
    }

    private fun repository(api: FakeFxApi = FakeFxApi()) =
        FxRepositoryImpl(api = api, config = ObpConfig(bankId = "ac.bank.uk"))

    @Test
    fun supportedCurrenciesUsesApiListWhenPresent() = runTest {
        val api = FakeFxApi(
            currencies = NetworkResult.Success(
                CurrenciesResponse(listOf(BankCurrency("GBP"), BankCurrency("EUR"), BankCurrency(""))),
            ),
        )
        assertEquals(listOf("GBP", "EUR"), repository(api).supportedCurrencies())
    }

    @Test
    fun supportedCurrenciesFallsBackWhenApiListEmpty() = runTest {
        val currencies = repository().supportedCurrencies()
        assertTrue(currencies.containsAll(listOf("GBP", "EUR", "USD", "JPY", "INR", "AUD", "AED")))
    }

    @Test
    fun supportedCurrenciesFallsBackWhenApiFails() = runTest {
        val api = FakeFxApi(currencies = NetworkResult.Error(NetworkError.UNAUTHORIZED))
        assertTrue(repository(api).supportedCurrencies().contains("GBP"))
    }

    @Test
    fun getRateMapsApiResult() = runTest {
        val rate = repository().getRate("GBP", "EUR").getOrThrow()
        assertEquals(1.16278, rate.conversionValue)
    }
}
