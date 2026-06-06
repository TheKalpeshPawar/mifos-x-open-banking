/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.pfm

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.model.obp.UserAttribute
import org.mifosx.openbanking.core.model.obp.UserAttributeRequest
import org.mifosx.openbanking.core.model.obp.UserAttributesResponse
import org.mifosx.openbanking.core.network.api.UserAttributesApi
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeUserAttributesApi(
    var attributes: List<UserAttribute> = emptyList(),
) : UserAttributesApi {
    var lastRequest: UserAttributeRequest? = null
    var listFails = false

    override suspend fun listPersonalDataFields(): NetworkResult<UserAttributesResponse, NetworkError> =
        if (listFails) {
            NetworkResult.Error(NetworkError.UNAUTHORIZED)
        } else {
            NetworkResult.Success(UserAttributesResponse(attributes))
        }

    override suspend fun createPersonalDataField(
        request: UserAttributeRequest,
    ): NetworkResult<UserAttribute, NetworkError> {
        lastRequest = request
        return NetworkResult.Success(UserAttribute(name = request.name, value = request.value))
    }
}

class BudgetsRepositoryTest {

    @Test
    fun budgetsParsesCategoryAndOverallLimits() = runTest {
        val api = FakeUserAttributesApi(
            attributes = listOf(
                UserAttribute(name = "pfm_budget_food-dining", value = "350.00", insertDate = "2026-06-01T00:00:00Z"),
                UserAttribute(name = "pfm_budget_overall", value = "1500.00", insertDate = "2026-06-01T00:00:00Z"),
                UserAttribute(name = "OPEY_CONSENT_TTL_SECONDS", value = "7776000"),
            ),
        )
        val budgets = BudgetsRepositoryImpl(api).budgets().getOrThrow()
        assertEquals(mapOf("food-dining" to 350.0), budgets.categoryLimits)
        assertEquals(1500.0, budgets.overallLimit)
    }

    @Test
    fun budgetsTakesNewestValuePerName() = runTest {
        val api = FakeUserAttributesApi(
            attributes = listOf(
                UserAttribute(name = "pfm_budget_bills", value = "500.00", insertDate = "2026-06-05T00:00:00Z"),
                UserAttribute(name = "pfm_budget_bills", value = "300.00", insertDate = "2026-06-01T00:00:00Z"),
            ),
        )
        val budgets = BudgetsRepositoryImpl(api).budgets().getOrThrow()
        assertEquals(500.0, budgets.categoryLimits["bills"])
    }

    @Test
    fun budgetsIgnoresUnparseableAndNonPositiveValues() = runTest {
        val api = FakeUserAttributesApi(
            attributes = listOf(
                UserAttribute(name = "pfm_budget_probe", value = "not-a-number"),
                UserAttribute(name = "pfm_budget_shopping", value = "0"),
            ),
        )
        val budgets = BudgetsRepositoryImpl(api).budgets().getOrThrow()
        assertTrue(budgets.categoryLimits.isEmpty())
        assertNull(budgets.overallLimit)
    }

    @Test
    fun budgetsFailurePropagates() = runTest {
        val api = FakeUserAttributesApi().apply { listFails = true }
        assertTrue(BudgetsRepositoryImpl(api).budgets().isFailure)
    }

    @Test
    fun saveBudgetPostsPrefixedNameAndFormattedAmount() = runTest {
        val api = FakeUserAttributesApi()
        BudgetsRepositoryImpl(api).saveBudget("food-dining", 350.0).getOrThrow()
        assertEquals("pfm_budget_food-dining", api.lastRequest?.name)
        assertEquals("350.00", api.lastRequest?.value)
        assertEquals("STRING", api.lastRequest?.type)
    }
}
