/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.CreateCounterpartyRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAccountsRepository(
    var accounts: Result<List<Account>>,
) : AccountsRepository {
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO()
    override suspend fun listAccounts(): Result<List<Account>> = accounts
    override suspend fun myAccounts(): Result<List<Account>> = accounts
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> =
        accounts.map { list -> list.firstOrNull { it.accountIdOrId == accountId } ?: account() }
}

private class FakePaymentsRepository(
    var beneficiaries: Result<List<Counterparty>> = Result.success(emptyList()),
    var funds: Result<Boolean> = Result.success(true),
    var sepa: Result<TransactionRequest> = Result.success(TransactionRequest(status = "COMPLETED")),
) : PaymentsRepository {
    override fun beneficiariesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Counterparty>> = TODO()
    override suspend fun listBeneficiaries(bankId: String, accountId: String): Result<List<Counterparty>> =
        beneficiaries
    override suspend fun listTransactionRequests(
        bankId: String,
        accountId: String,
    ): Result<List<TransactionRequestSummary>> = Result.success(emptyList())
    override suspend fun createBeneficiary(
        bankId: String,
        accountId: String,
        request: CreateCounterpartyRequest,
    ): Result<Counterparty> = TODO()
    override suspend fun sendSepaPayment(
        bankId: String,
        accountId: String,
        iban: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> = sepa
    override suspend fun sendToCounterparty(
        bankId: String,
        accountId: String,
        counterpartyId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> = sepa
    override suspend fun fundsAvailable(
        bankId: String,
        accountId: String,
        amount: String,
        currency: String,
    ): Result<Boolean> = funds
    override suspend fun checkIban(iban: String): Result<Boolean> = Result.success(true)
}

private fun account() = Account(
    id = "ac.checking.001",
    bankId = "ac.bank.uk",
    label = "Main",
    balance = AmountOfMoney(currency = "EUR", amount = "100"),
)

private fun payee(id: String, name: String) = Counterparty(
    counterpartyId = id,
    name = name,
    otherAccountRoutingScheme = "IBAN",
    otherAccountRoutingAddress = "DE89370400440532099999",
    isBeneficiary = true,
)

class SendMoneyViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        accounts: Result<List<Account>> = Result.success(listOf(account())),
        payments: FakePaymentsRepository = FakePaymentsRepository(
            beneficiaries = Result.success(listOf(payee("b1", "TechStart Ltd"))),
        ),
    ) = SendMoneyViewModel(FakeAccountsRepository(accounts), payments)

    @Test
    fun load_success_emitsContent() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content)
        assertEquals("EUR", s.data.currency)
        assertEquals(1, s.data.beneficiaries.size)
    }

    @Test
    fun continue_producesDraft_whenValid() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onBeneficiarySelected("b1")
        model.onAmountChanged("25.50")
        model.onReferenceChanged("rent")
        advanceUntilIdle()

        var draft: PaymentDraft? = null
        model.onContinue { draft = it }
        advanceUntilIdle()

        assertEquals("25.50", draft?.amount)
        assertEquals("TechStart Ltd", draft?.beneficiaryName)
        assertEquals("DE89370400440532099999", draft?.iban)
        assertEquals("ac.bank.uk", draft?.fromBankId)
    }

    @Test
    fun continue_blocksWithError_whenInsufficientFunds() = runTest(dispatcher) {
        val payments = FakePaymentsRepository(
            beneficiaries = Result.success(listOf(payee("b1", "TechStart Ltd"))),
            funds = Result.success(false),
        )
        val model = vm(payments = payments)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onBeneficiarySelected("b1")
        model.onAmountChanged("25.50")
        advanceUntilIdle()

        var draft: PaymentDraft? = null
        model.onContinue { draft = it }
        advanceUntilIdle()

        assertNull(draft)
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content)
        assertEquals("Insufficient funds in your account.", s.data.formError)
    }
}
