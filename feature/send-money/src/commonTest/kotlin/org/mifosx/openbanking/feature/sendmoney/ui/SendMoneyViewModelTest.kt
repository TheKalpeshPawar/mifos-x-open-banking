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
import org.mifosx.openbanking.core.data.banks.BanksRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.Bank
import org.mifosx.openbanking.core.model.obp.BankAttribute
import org.mifosx.openbanking.core.model.obp.Counterparty
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
}

private class FakeBanksRepository(
    var banks: Map<String, Bank> = mapOf(
        "ac.bank.uk" to Bank(
            id = "ac.bank.uk",
            fullName = "Afternoon Coffee Bank",
            attributes = listOf(BankAttribute(name = "SWIFT_BIC", value = "ACMEGB2L")),
        ),
    ),
) : BanksRepository {
    override suspend fun bankName(bankId: String): String = banks[bankId]?.fullName ?: bankId
    override suspend fun bank(bankId: String): Bank? = banks[bankId]
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
        banks: FakeBanksRepository = FakeBanksRepository(),
    ) = SendMoneyViewModel(FakeAccountsRepository(accounts), payments, banks)

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
    fun setAccount_switchesBeneficiaries_andDropsForeignPayee() = runTest(dispatcher) {
        val savings = Account(
            id = "ac.savings.001",
            bankId = "ac.bank.uk",
            label = "Savings",
            balance = AmountOfMoney(currency = "EUR", amount = "50"),
        )
        val payments = FakePaymentsRepository(
            beneficiaries = Result.success(listOf(payee("b1", "TechStart Ltd"))),
        )
        val model = vm(
            accounts = Result.success(listOf(account(), savings)),
            payments = payments,
        )
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onBeneficiarySelected("b1")
        advanceUntilIdle()

        payments.beneficiaries = Result.success(listOf(payee("b2", "Acme Supplies")))
        model.setAccount("ac.savings.001")
        advanceUntilIdle()

        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals("ac.savings.001", c.selectedAccount.accountIdOrId)
        assertEquals(listOf("b2"), c.beneficiaries.map { it.counterpartyId })
        assertNull(c.selectedBeneficiary)
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

    @Test
    fun selectingBeneficiary_autoSelectsRecommendedRail() = runTest(dispatcher) {
        // EUR account at a GB bank paying a DE IBAN -> SEPA recommended (free, no conversion).
        val model = vm()
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onBeneficiarySelected("b1")
        advanceUntilIdle()

        val s = model.uiState.value
        assertTrue(s is ScreenState.Content)
        assertEquals(PaymentType.SEPA, s.data.paymentType)
        val sepa = s.data.railAssessments.getValue(PaymentType.SEPA)
        assertTrue(sepa.eligible)
        assertNull(sepa.conversionNote)
        // DE recipient from a GB bank -> domestic disabled with a reason.
        val domestic = s.data.railAssessments.getValue(PaymentType.DOMESTIC)
        assertTrue(!domestic.eligible && domestic.reason != null)
    }

    @Test
    fun paymentTypeChange_ignoredForIneligibleRail() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onBeneficiarySelected("b1")
        advanceUntilIdle()

        model.onPaymentTypeChanged(PaymentType.DOMESTIC)
        advanceUntilIdle()

        val s = model.uiState.value
        assertTrue(s is ScreenState.Content)
        assertEquals(PaymentType.SEPA, s.data.paymentType)
    }

    @Test
    fun draft_carriesRailAndConversionNote() = runTest(dispatcher) {
        // GBP source account -> SEPA stays recommended for the DE IBAN, with a conversion note.
        val gbpAccount = account().copy(balance = AmountOfMoney(currency = "GBP", amount = "100"))
        val model = vm(accounts = Result.success(listOf(gbpAccount)))
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onBeneficiarySelected("b1")
        model.onAmountChanged("10")
        advanceUntilIdle()

        var draft: PaymentDraft? = null
        model.onContinue { draft = it }
        advanceUntilIdle()

        assertEquals(PaymentType.SEPA, draft?.paymentType)
        assertEquals("Converted GBP→EUR · FX fee applies", draft?.conversionNote)
    }
}
