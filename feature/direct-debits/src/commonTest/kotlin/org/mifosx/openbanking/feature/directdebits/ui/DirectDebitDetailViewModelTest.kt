/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.directdebits.DirectDebitsRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.DirectDebitCollection
import org.mifosx.openbanking.core.model.obp.DirectDebitMandate
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun netflixMandate(status: String = DirectDebitMandate.STATUS_ACTIVE) = DirectDebitMandate(
    id = "dd-netflix",
    merchantName = "Netflix Subscription",
    amountValue = "15.99",
    amountCurrency = "GBP",
    frequency = "MONTHLY",
    lastCollectionDate = "2026-05-15",
    nextCollectionDate = if (status == DirectDebitMandate.STATUS_ACTIVE) "2026-06-15" else "",
    status = status,
    mandateReference = "DD-NS-20240112",
    firstCollectionDate = "2024-01-12",
    recentCollections = listOf(
        DirectDebitCollection("2026-05-15", "15.99", "GBP"),
        DirectDebitCollection("2026-04-15", "15.99", "GBP"),
    ),
)

private class DetailFakeRepository(
    var mandates: List<DirectDebitMandate> = listOf(netflixMandate()),
    var failure: Throwable? = null,
) : DirectDebitsRepository {
    val cancelledIds = mutableListOf<String>()

    override suspend fun listMandates(bankId: String, accountId: String): Result<List<DirectDebitMandate>> {
        failure?.let { return Result.failure(it) }
        return Result.success(
            mandates.map { m ->
                if (m.id in cancelledIds) {
                    m.copy(status = DirectDebitMandate.STATUS_CANCELLED, nextCollectionDate = "")
                } else {
                    m
                }
            },
        )
    }

    override suspend fun cancel(accountId: String, mandateId: String): Result<Unit> {
        cancelledIds += mandateId
        return Result.success(Unit)
    }
}

private class DetailFakeAccountsRepository(
    private val account: Account? = Account(
        id = "ac.checking.001",
        bankId = "ac.bank.uk",
        label = "Everyday Current",
        accountType = "CHECKING",
        balance = AmountOfMoney(currency = "GBP", amount = "1200.00"),
    ),
) : AccountsRepository {
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO("not used")
    override suspend fun listAccounts(): Result<List<Account>> = TODO("not used")
    override suspend fun myAccounts(): Result<List<Account>> = TODO("not used")
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> =
        account?.let { Result.success(it) } ?: Result.failure(IllegalStateException("no detail"))
}

class DirectDebitDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        repository: DetailFakeRepository = DetailFakeRepository(),
        accounts: DetailFakeAccountsRepository = DetailFakeAccountsRepository(),
        mandateId: String = "dd-netflix",
    ) = DirectDebitDetailViewModel(
        directDebitsRepository = repository,
        accountsRepository = accounts,
        bankId = "ac.bank.uk",
        accountId = "ac.checking.4521",
        mandateId = mandateId,
    )

    private suspend fun TestScope.content(model: DirectDebitDetailViewModel): DirectDebitDetailContent {
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content, "expected Content, was $s")
        return s.data
    }

    @Test
    fun load_found_projectsMandateDetail() = runTest(dispatcher) {
        val c = content(vm())
        assertEquals("Netflix Subscription", c.merchantName)
        assertEquals("Active", c.statusLabel)
        assertTrue(c.isActive)
        assertEquals("£15.99", c.amountLabel)
        assertEquals("Monthly", c.frequencyLabel)
        assertEquals("15 Jun 2026", c.nextPaymentLabel)
        assertEquals("Everyday Current", c.linkedAccountName)
        assertEquals("••••4521", c.linkedAccountMasked)
        assertEquals("DD-NS-20240112", c.mandateReference)
        assertEquals("12 Jan 2024", c.startDateLabel)
        assertEquals(2, c.recentPayments.size)
        assertEquals("15 May 2026", c.recentPayments.first().dateLabel)
        assertEquals("-£15.99", c.recentPayments.first().amountLabel)
    }

    @Test
    fun load_missingMandate_isEmpty() = runTest(dispatcher) {
        val model = vm(mandateId = "dd-unknown")
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Empty)
    }

    @Test
    fun load_failure_isError_andRetryRecovers() = runTest(dispatcher) {
        val repository = DetailFakeRepository(failure = IllegalStateException("boom"))
        val model = vm(repository)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)

        repository.failure = null
        model.onRetry()
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Content)
    }

    @Test
    fun cancelFlow_confirm_flipsToCancelled() = runTest(dispatcher) {
        val repository = DetailFakeRepository()
        val model = vm(repository)
        content(model)

        model.onCancelRequested()
        advanceUntilIdle()
        assertTrue((model.uiState.value as ScreenState.Content).data.cancelDialogVisible)

        model.onCancelConfirmed()
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("dd-netflix"), repository.cancelledIds)
        assertFalse(c.isActive)
        assertEquals("Cancelled", c.statusLabel)
        assertFalse(c.cancelDialogVisible)
        assertEquals("—", c.nextPaymentLabel)
        assertEquals(2, c.recentPayments.size)
    }

    @Test
    fun cancelRequest_onCancelledMandate_isIgnored() = runTest(dispatcher) {
        val repository = DetailFakeRepository(
            mandates = listOf(netflixMandate(status = DirectDebitMandate.STATUS_CANCELLED)),
        )
        val model = vm(repository)
        content(model)
        model.onCancelRequested()
        advanceUntilIdle()
        assertFalse((model.uiState.value as ScreenState.Content).data.cancelDialogVisible)
    }

    @Test
    fun cancelDismissed_closesDialogWithoutCancelling() = runTest(dispatcher) {
        val repository = DetailFakeRepository()
        val model = vm(repository)
        content(model)
        model.onCancelRequested()
        advanceUntilIdle()
        model.onCancelDismissed()
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertFalse(c.cancelDialogVisible)
        assertTrue(repository.cancelledIds.isEmpty())
        assertTrue(c.isActive)
    }

    @Test
    fun accountDetailFailure_fallsBackGracefully() = runTest(dispatcher) {
        val c = content(vm(accounts = DetailFakeAccountsRepository(account = null)))
        assertEquals("Account", c.linkedAccountName)
        assertEquals("••••4521", c.linkedAccountMasked)
    }
}
