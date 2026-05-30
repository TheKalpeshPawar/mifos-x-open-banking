/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.payments

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.model.obp.CounterpartiesResponse
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.network.api.PaymentsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakePaymentsApi(
    var listResult: NetworkResult<CounterpartiesResponse, NetworkError> =
        NetworkResult.Success(CounterpartiesResponse()),
) : PaymentsApi {
    override suspend fun listCounterparties(bankId: String, accountId: String) = listResult
}

class PaymentsRepositoryTest {

    @Test
    fun listBeneficiaries_unwrapsList() = runTest {
        val repo = PaymentsRepositoryImpl(
            FakePaymentsApi(
                NetworkResult.Success(
                    CounterpartiesResponse(counterparties = listOf(Counterparty(counterpartyId = "b1"))),
                ),
            ),
            ObpConfig(),
        )
        val result = repo.listBeneficiaries("acc-1")
        assertTrue(result.isSuccess)
        assertEquals("b1", result.getOrNull()?.firstOrNull()?.counterpartyId)
    }

    @Test
    fun listBeneficiaries_error_isFailure() = runTest {
        val repo = PaymentsRepositoryImpl(FakePaymentsApi(NetworkResult.Error(NetworkError.SERVER)), ObpConfig())
        assertTrue(repo.listBeneficiaries("acc-1").isFailure)
    }
}
