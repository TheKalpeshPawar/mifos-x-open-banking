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

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.FxRate
import org.mifosx.openbanking.core.network.api.FxApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read access to OBP foreign-exchange rates. */
interface FxRepository {
    suspend fun getRate(from: String, to: String): Result<FxRate>

    /**
     * ISO currency codes the FX converter can offer. Backed by the bank-currencies endpoint;
     * the sandbox returns an empty list for every bank, so an empty or failed response falls
     * back to the codes the sandbox FX engine actually accepts (probed live — pairs outside
     * this set answer 400). Never empty.
     */
    suspend fun supportedCurrencies(): List<String>
}

class FxRepositoryImpl(
    private val api: FxApi,
    private val config: ObpConfig,
) : FxRepository {

    override suspend fun getRate(from: String, to: String): Result<FxRate> =
        api.getRate(config.bankId, from, to).toResult()

    override suspend fun supportedCurrencies(): List<String> {
        val fromApi = api.getCurrencies(config.bankId).toResult()
            .getOrNull()?.currencies.orEmpty()
            .map { it.alphanumericCode }
            .filter { it.isNotBlank() }
        return fromApi.ifEmpty { FALLBACK_CURRENCIES }
    }

    private companion object {
        val FALLBACK_CURRENCIES = listOf("GBP", "EUR", "USD", "JPY", "INR", "AUD", "AED")
    }
}
