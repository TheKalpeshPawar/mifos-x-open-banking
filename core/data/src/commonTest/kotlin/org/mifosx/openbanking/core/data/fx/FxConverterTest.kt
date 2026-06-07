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
import org.mifosx.openbanking.core.model.obp.FxRate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FxFakeRepository(
    var result: Result<FxRate> = Result.success(FxRate(conversionValue = 1.16278)),
) : FxRepository {
    var calls = 0
    override suspend fun getRate(from: String, to: String): Result<FxRate> {
        calls++
        return result
    }
}

class FxConverterTest {

    @Test
    fun rate_identityPairSkipsNetwork() = runTest {
        val repo = FxFakeRepository()
        val converter = FxConverter(repo)

        assertEquals(1.0, converter.rate("EUR", "EUR"))
        assertEquals(0, repo.calls)
    }

    @Test
    fun rate_fetchesThenCaches() = runTest {
        val repo = FxFakeRepository()
        val converter = FxConverter(repo)

        assertEquals(1.16278, converter.rate("GBP", "EUR"))
        assertEquals(1.16278, converter.rate("GBP", "EUR"))
        assertEquals(1, repo.calls)
    }

    @Test
    fun rate_failureReturnsNullAndRetriesNextCall() = runTest {
        val repo = FxFakeRepository(result = Result.failure(IllegalStateException("offline")))
        val converter = FxConverter(repo)

        assertNull(converter.rate("GBP", "EUR"))
        repo.result = Result.success(FxRate(conversionValue = 1.2))
        assertEquals(1.2, converter.rate("GBP", "EUR"))
        assertEquals(2, repo.calls)
    }

    @Test
    fun rate_blankCurrencyTreatedAsIdentity() = runTest {
        val converter = FxConverter(FxFakeRepository())

        assertEquals(1.0, converter.rate("", "EUR"))
    }
}
