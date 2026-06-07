/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.accounts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.pfm.PfmScope
import org.mifosx.openbanking.core.model.pfm.pfmScope
import template.core.base.store.screen.ScreenDataStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun listAccount(id: String, bankId: String = "bank-1") = Account(
    id = id,
    bankId = bankId,
    label = "Account $id",
)

private fun detailAccount(id: String, productCode: String, currency: String = "EUR") = Account(
    accountId = id,
    productCode = productCode,
    balance = AmountOfMoney(currency = currency, amount = "100.00"),
)

private class SvcFakeAccountsRepository(
    var listResult: Result<List<Account>> = Result.success(emptyList()),
    var details: Map<String, Result<Account>> = emptyMap(),
) : AccountsRepository {
    var detailCalls = 0
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO()
    override suspend fun listAccounts(): Result<List<Account>> = TODO()
    override suspend fun myAccounts(): Result<List<Account>> = listResult
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> {
        detailCalls++
        return details[accountId] ?: Result.failure(IllegalStateException("no detail for $accountId"))
    }
}

class PfmAccountsServiceTest {

    @Test
    fun classifiedAccounts_mergesDetailProductCode() = runTest {
        val repo = SvcFakeAccountsRepository(
            listResult = Result.success(listOf(listAccount("acc-1"), listAccount("biz-1"))),
            details = mapOf(
                "acc-1" to Result.success(detailAccount("acc-1", "CURRENT")),
                "biz-1" to Result.success(detailAccount("biz-1", "BUSINESS", currency = "GBP")),
            ),
        )
        val service = PfmAccountsService(repo)

        val accounts = service.classifiedAccounts().getOrThrow()

        assertEquals(PfmScope.PERSONAL, accounts.first { it.id == "acc-1" }.pfmScope)
        assertEquals(PfmScope.BUSINESS, accounts.first { it.id == "biz-1" }.pfmScope)
        assertEquals("bank-1", accounts.first { it.id == "biz-1" }.bankId)
        assertEquals("GBP", accounts.first { it.id == "biz-1" }.balance.currency)
    }

    @Test
    fun classifiedAccounts_cachesAcrossCalls() = runTest {
        val repo = SvcFakeAccountsRepository(
            listResult = Result.success(listOf(listAccount("acc-1"))),
            details = mapOf("acc-1" to Result.success(detailAccount("acc-1", "CURRENT"))),
        )
        val service = PfmAccountsService(repo)

        service.classifiedAccounts().getOrThrow()
        service.classifiedAccounts().getOrThrow()

        assertEquals(1, repo.detailCalls)
    }

    @Test
    fun classifiedAccounts_invalidateDropsCache() = runTest {
        val repo = SvcFakeAccountsRepository(
            listResult = Result.success(listOf(listAccount("acc-1"))),
            details = mapOf("acc-1" to Result.success(detailAccount("acc-1", "CURRENT"))),
        )
        val service = PfmAccountsService(repo)

        service.classifiedAccounts().getOrThrow()
        service.invalidate()
        service.classifiedAccounts().getOrThrow()

        assertEquals(2, repo.detailCalls)
    }

    @Test
    fun classifiedAccounts_detailFailureDegradesToListShape() = runTest {
        val repo = SvcFakeAccountsRepository(
            listResult = Result.success(listOf(listAccount("acc-1"), listAccount("acc-2"))),
            details = mapOf("acc-1" to Result.success(detailAccount("acc-1", "SAVINGS"))),
        )
        val service = PfmAccountsService(repo)

        val accounts = service.classifiedAccounts().getOrThrow()

        assertEquals(2, accounts.size)
        assertEquals("SAVINGS", accounts.first { it.id == "acc-1" }.productCode)
        assertTrue(accounts.first { it.id == "acc-2" }.productCode.isBlank())
        assertEquals(PfmScope.PERSONAL, accounts.first { it.id == "acc-2" }.pfmScope)
    }

    @Test
    fun classifiedAccounts_listFailurePropagates() = runTest {
        val repo = SvcFakeAccountsRepository(listResult = Result.failure(IllegalStateException("offline")))
        val service = PfmAccountsService(repo)

        assertTrue(service.classifiedAccounts().isFailure)
    }
}
