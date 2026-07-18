/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.standingorders.StandingOrdersRepository
import org.mifosx.openbanking.core.model.obp.CreateStandingOrderRequest
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.model.obp.StandingOrderDetail
import org.mifosx.openbanking.core.model.obp.StandingOrderExecution
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun detailOrder(
    id: String = "so-derived-rent",
    name: String = "Rent",
    status: String = StandingOrder.STATUS_ACTIVE,
    counterpartyName: String = "Landlord Holdings Ltd",
    counterpartyAccount: String = "ac.savings.001",
    amount: String = "450.00",
    currency: String = "EUR",
    frequency: String = "MONTHLY",
    lastPaymentDate: String = "2026-06-01",
    nextPaymentDate: String = "2026-07-01",
    created: Boolean = false,
) = StandingOrder(
    id = id,
    name = name,
    counterpartyName = counterpartyName,
    counterpartyAccount = counterpartyAccount,
    amountValue = amount,
    amountCurrency = currency,
    frequency = frequency,
    lastPaymentDate = lastPaymentDate,
    nextPaymentDate = nextPaymentDate,
    status = status,
    created = created,
)

private fun execution(date: String, id: String = "tx-$date") =
    StandingOrderExecution(transactionId = id, date = date, amount = "450.00", currency = "EUR")

private class SodFakeStandingOrdersRepository(
    var result: Result<StandingOrderDetail> = Result.success(
        StandingOrderDetail(detailOrder(), listOf(execution("2026-06-01"), execution("2026-05-01"))),
    ),
) : StandingOrdersRepository {
    var calls = 0
    override suspend fun listRecurring(bankId: String, accountId: String): Result<List<StandingOrder>> = TODO()
    override suspend fun detail(
        bankId: String,
        accountId: String,
        standingOrderId: String,
    ): Result<StandingOrderDetail> {
        calls++
        return result
    }
    override suspend fun create(
        bankId: String,
        accountId: String,
        name: String,
        request: CreateStandingOrderRequest,
    ): Result<StandingOrder> = TODO()
}

class StandingOrderDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(repository: SodFakeStandingOrdersRepository = SodFakeStandingOrdersRepository()) =
        StandingOrderDetailViewModel(
            standingOrdersRepository = repository,
            bankId = "ac.bank.uk",
            accountId = "acc-1",
            standingOrderId = "so-derived-rent",
        )

    private suspend fun TestScope.content(model: StandingOrderDetailViewModel): StandingOrderDetailContent {
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content, "expected Content, was $s")
        return s.data
    }

    @Test
    fun load_derivesDisplayContent() = runTest(dispatcher) {
        val c = content(vm())
        assertEquals("Rent", c.name)
        assertEquals("Active", c.statusLabel)
        assertEquals("Landlord Holdings Ltd", c.recipientName)
        assertEquals("····s001", c.recipientAccount)
        assertEquals("€450.00", c.amount)
        assertEquals("EUR", c.currency)
        assertEquals("Monthly", c.frequencyLabel)
        assertEquals("1 May 2026", c.startedOn)
        assertEquals("1 Jun 2026", c.lastPayment)
        assertEquals("1 Jul 2026", c.nextPayment)
        assertEquals("Ongoing", c.finalDate)
        assertEquals(2, c.executions.size)
        assertEquals("1 Jun 2026", c.executions.first().dateLabel)
        assertEquals("€450.00", c.executions.first().amountLabel)
        assertEquals("tx-2026-06-01", c.executions.first().transactionId)
    }

    @Test
    fun load_executionsCappedAtFive() = runTest(dispatcher) {
        val executions = (1..7).map { execution("2026-0$it-01".take(10)) }
        val c = content(
            vm(SodFakeStandingOrdersRepository(Result.success(StandingOrderDetail(detailOrder(), executions)))),
        )
        assertEquals(5, c.executions.size)
    }

    @Test
    fun load_pausedStatusLabel() = runTest(dispatcher) {
        val c = content(
            vm(
                SodFakeStandingOrdersRepository(
                    Result.success(StandingOrderDetail(detailOrder(status = StandingOrder.STATUS_PAUSED), emptyList())),
                ),
            ),
        )
        assertEquals("Paused", c.statusLabel)
    }

    @Test
    fun load_createdOrderHasNoHistoryAndNoStart() = runTest(dispatcher) {
        val created = detailOrder(
            id = "so-1",
            created = true,
            lastPaymentDate = "",
            nextPaymentDate = "2026-07-01",
        )
        val c = content(
            vm(SodFakeStandingOrdersRepository(Result.success(StandingOrderDetail(created, emptyList())))),
        )
        assertTrue(c.executions.isEmpty())
        assertEquals("", c.startedOn)
        assertEquals("", c.lastPayment)
        assertEquals("1 Jul 2026", c.nextPayment)
    }

    @Test
    fun load_blankRecipientFallsBackToName() = runTest(dispatcher) {
        val c = content(
            vm(
                SodFakeStandingOrdersRepository(
                    Result.success(
                        StandingOrderDetail(detailOrder(counterpartyName = "", counterpartyAccount = ""), emptyList()),
                    ),
                ),
            ),
        )
        assertEquals("Rent", c.recipientName)
        assertEquals("", c.recipientAccount)
    }

    @Test
    fun load_failureIsError() = runTest(dispatcher) {
        val model = vm(SodFakeStandingOrdersRepository(Result.failure(RuntimeException("boom"))))
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)
    }

    @Test
    fun retry_afterFailureLoadsContent() = runTest(dispatcher) {
        val repository = SodFakeStandingOrdersRepository(Result.failure(RuntimeException("boom")))
        val model = vm(repository)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)

        repository.result = Result.success(StandingOrderDetail(detailOrder(), emptyList()))
        model.onRetry()
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Content)
        assertEquals(2, repository.calls)
    }
}
