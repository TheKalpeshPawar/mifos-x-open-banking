/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.banking.payment.ChargeBearer
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
import org.mifosx.openbanking.feature.sendmoney.FakeAccountCapabilityRegistry
import org.mifosx.openbanking.feature.sendmoney.FakeAccountsOverviewRepository
import org.mifosx.openbanking.feature.sendmoney.FakeBeneficiariesRepository
import org.mifosx.openbanking.feature.sendmoney.FakePaymentInitiationRepository
import org.mifosx.openbanking.feature.sendmoney.SendMoneyFixtures
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What each rail asks for, and what it refuses.
 *
 * Split from [SendMoneyViewModelTest] because it is one coherent question — the two rails accept
 * genuinely different fields, and sending the wrong one is a hard 400 rather than a preference.
 */
class SendMoneyRailTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val registry = FakeAccountCapabilityRegistry()

    private fun viewModel(
        payments: FakePaymentInitiationRepository = FakePaymentInitiationRepository(),
    ) = SendMoneyViewModel(
        FakeAccountsOverviewRepository(),
        FakeBeneficiariesRepository(),
        payments,
        registry,
    )

    private fun content(vm: SendMoneyViewModel): SendMoneyUiState.Content =
        assertIs<SendMoneyUiState.Content>(vm.stateFlow.value.uiState)

    /** Walks the domestic form to review, for the cases that compare against it. */
    private fun SendMoneyViewModel.completeForm() {
        trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))
        trySendAction(SendMoneyAction.SelectCreditor(SendMoneyFixtures.JAMESON_ID))
        trySendAction(SendMoneyAction.EnterAmount("850"))
        trySendAction(SendMoneyAction.ReviewPayment)
    }

    /**
     * Switching rails invalidates every payee-shaped field.
     *
     * A payee is identified by sort code and account number on one rail and by IBAN on the other,
     * so one is not the other, and half-typed fields for the wrong scheme would otherwise be carried
     * into a request that refuses them with `U027`.
     */
    @Test
    fun switchingRailsClearsThePayeeAndEverythingTypedTowardsOne() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))
        vm.trySendAction(SendMoneyAction.SelectCreditor(SendMoneyFixtures.JAMESON_ID))
        vm.trySendAction(SendMoneyAction.EnterManualSortCode("4012"))

        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))

        val state = content(vm)
        assertNull(state.creditor)
        assertEquals("", state.manualSortCode)
        assertEquals("", state.manualAccountNumber)
        assertEquals("", state.manualIban)
        assertTrue(state.fieldErrors.isEmpty)
    }

    /** The amount means the same on both rails, so it is not thrown away. */
    @Test
    fun switchingRailsKeepsTheAmount() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))
        vm.trySendAction(SendMoneyAction.EnterAmount("850"))

        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))

        assertEquals("850", content(vm).amountInput)
    }

    /**
     * Sterling survives the switch, and the currencies offered are HSBC's whole routing list.
     *
     * This asserted the opposite — that switching moved the transfer currency off GBP onto USD —
     * because the currency list was believed to be a two-value Global Money allowlist. INT-04
     * disproves it: `CurrencyOfTransfer: GBP` stages `201`/`AWAU`. So GBP is a legitimate
     * international choice, and rewriting it on the way in changed a decision nobody made.
     */
    @Test
    fun switchingToInternationalKeepsSterlingAndOffersTheWholeRoutingList() = runTest {
        val vm = viewModel()
        assertEquals("GBP", content(vm).currencyOfTransfer)

        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))

        val state = content(vm)
        assertEquals("GBP", state.currencyOfTransfer)
        assertEquals("GBP", state.instructedCurrency)
        assertContentEquals(OFFERED_CURRENCIES, state.offeredCurrencies)
        assertEquals(19, state.offeredCurrencies.size)
    }

    /** Neither currency is offered domestically, so both are put back before the rail is shown. */
    @Test
    fun switchingBackToDomesticNormalisesBothCurrenciesToSterling() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))
        vm.trySendAction(SendMoneyAction.SelectCurrencyOfTransfer("USD"))
        vm.trySendAction(SendMoneyAction.SelectInstructedCurrency("USD"))

        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.Domestic))

        val state = content(vm)
        assertEquals("GBP", state.instructedCurrency)
        assertEquals("GBP", state.currencyOfTransfer)
        assertTrue(state.offeredCurrencies.isEmpty())
    }

    /**
     * `InstructedAmount.Currency` follows the selector, and `buildDraft` carries what it says.
     *
     * It was the constant `INSTRUCTED_CURRENCY = "GBP"`, so nothing the customer picked could reach
     * the wire.
     */
    @Test
    fun anInternationalDraftInstructsInTheChosenCurrency() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)

        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))
        vm.trySendAction(SendMoneyAction.EnterManualName("Klara Weiss"))
        vm.trySendAction(SendMoneyAction.EnterManualIban("DE89 3704 0044 0532 0130 00"))
        vm.trySendAction(SendMoneyAction.ConfirmManualCreditor)
        vm.trySendAction(SendMoneyAction.EnterAmount("250"))
        vm.trySendAction(SendMoneyAction.SelectCurrencyOfTransfer("USD"))
        vm.trySendAction(SendMoneyAction.SelectInstructedCurrency("USD"))
        vm.trySendAction(SendMoneyAction.ReviewPayment)
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        assertEquals("USD", payments.stagedDrafts.single().currency)
    }

    /**
     * The combination HSBC refuses gates the review rather than warning beside it.
     *
     * `InstructedAmount.Currency` must equal the debtor account's currency or the currency of
     * transfer. A sterling account, dollars instructed, euros received is neither, and the only thing
     * between the customer and a `400` is this.
     */
    @Test
    fun aCurrencyCombinationTheBankRefusesBlocksTheReview() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))
        vm.trySendAction(SendMoneyAction.EnterManualName("Klara Weiss"))
        vm.trySendAction(SendMoneyAction.EnterManualIban("DE89 3704 0044 0532 0130 00"))
        vm.trySendAction(SendMoneyAction.ConfirmManualCreditor)
        vm.trySendAction(SendMoneyAction.EnterAmount("250"))
        vm.trySendAction(SendMoneyAction.SelectCurrencyOfTransfer("EUR"))

        vm.trySendAction(SendMoneyAction.SelectInstructedCurrency("USD"))

        val refused = content(vm)
        assertFalse(refused.instructedCurrencyValid)
        assertFalse(refused.canReview)

        // Matching either side of the pair is enough; here it is the currency of transfer.
        vm.trySendAction(SendMoneyAction.SelectCurrencyOfTransfer("USD"))

        val accepted = content(vm)
        assertTrue(accepted.instructedCurrencyValid)
        assertTrue(accepted.canReview)
    }

    /** The other of the two clauses: instructing in the payer account's own currency. */
    @Test
    fun instructingInThePayerAccountsCurrencyIsAccepted() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))
        vm.trySendAction(SendMoneyAction.SelectCurrencyOfTransfer("EUR"))

        assertTrue(content(vm).instructedCurrencyValid)
    }

    /**
     * With no payer there is no debtor currency, so only the transfer clause can hold.
     *
     * A blank debtor currency must not be treated as matching anything — that would let the one
     * combination the bank refuses through on the rail where the customer has told us least.
     */
    @Test
    fun withNoPayerOnlyTheTransferCurrencyCanJustifyTheAmountsCurrency() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.LetBankChoosePayer)
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))
        vm.trySendAction(SendMoneyAction.SelectCurrencyOfTransfer("EUR"))

        vm.trySendAction(SendMoneyAction.SelectInstructedCurrency("GBP"))
        assertFalse(content(vm).instructedCurrencyValid)

        vm.trySendAction(SendMoneyAction.SelectInstructedCurrency("EUR"))
        assertTrue(content(vm).instructedCurrencyValid)
    }

    /**
     * The balance rung is skipped, not adapted, once the currencies differ.
     *
     * £482.10 is available and 850 is typed, which would be refused as beyond the balance — but the
     * amount is 850 USD and the balance is sterling, and comparing the two numbers is a comparison
     * of nothing. The bank's funds confirmation remains the binding check.
     */
    @Test
    fun theBalanceCheckIsSkippedWhenTheAmountIsInAnotherCurrency() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.SAVINGS_ACCOUNT_ID))
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))
        vm.trySendAction(SendMoneyAction.EnterAmount("850"))
        assertEquals(SendMoneyAmountProblem.ExceedsAvailableBalance, content(vm).amountProblem)

        vm.trySendAction(SendMoneyAction.SelectInstructedCurrency("USD"))

        assertNull(content(vm).amountProblem)
    }

    /** And it comes back the moment the amount is denominated in the account's currency again. */
    @Test
    fun theBalanceCheckReturnsWhenTheAmountIsBackInTheAccountsCurrency() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.SAVINGS_ACCOUNT_ID))
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))
        vm.trySendAction(SendMoneyAction.SelectInstructedCurrency("USD"))
        vm.trySendAction(SendMoneyAction.EnterAmount("850"))
        assertNull(content(vm).amountProblem)

        vm.trySendAction(SendMoneyAction.SelectInstructedCurrency("GBP"))

        assertEquals(SendMoneyAmountProblem.ExceedsAvailableBalance, content(vm).amountProblem)
    }

    @Test
    fun anInternationalDraftCarriesTheChargesAndCurrencyAndNoReference() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)

        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))
        vm.trySendAction(SendMoneyAction.EnterManualName("Klara Weiss"))
        vm.trySendAction(SendMoneyAction.EnterManualIban("DE89 3704 0044 0532 0130 00"))
        vm.trySendAction(SendMoneyAction.ConfirmManualCreditor)
        vm.trySendAction(SendMoneyAction.EnterAmount("850"))
        vm.trySendAction(SendMoneyAction.SelectCurrencyOfTransfer("EUR"))
        vm.trySendAction(SendMoneyAction.SelectChargeBearer(ChargeBearer.Shared))
        vm.trySendAction(SendMoneyAction.ReviewPayment)
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val draft = payments.stagedDrafts.single()
        assertEquals("EUR", draft.currencyOfTransfer)
        assertEquals(ChargeBearer.Shared, draft.chargeBearer)
        assertNull(draft.reference)
        assertEquals(BeneficiaryScheme.Iban, draft.creditor.scheme)
        // Normalised: written down an IBAN carries spaces, and OBIE wants none.
        assertEquals("DE89370400440532013000", draft.creditor.identification)
    }

    @Test
    fun aDomesticDraftCarriesTheReferenceAndNeitherInternationalField() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)

        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val draft = payments.stagedDrafts.single()
        assertEquals("RENT-FLAT12", draft.reference)
        assertNull(draft.currencyOfTransfer)
        assertNull(draft.chargeBearer)
    }

    /** The old check accepted any five characters; this is the guard before a hand-keyed IBAN. */
    @Test
    fun refusesAnIbanThatCouldNotBeOne() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))

        vm.trySendAction(SendMoneyAction.EnterManualIban("DE89!!"))

        assertTrue(content(vm).fieldErrors.ibanInvalid)
    }

    @Test
    fun acceptsAnIbanWrittenWithSpacesAndInLowerCase() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))

        vm.trySendAction(SendMoneyAction.EnterManualIban("de89 3704 0044 0532 0130 00"))

        assertFalse(content(vm).fieldErrors.ibanInvalid)
    }

    @Test
    fun staysQuietOnAnEmptyIbanField() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))

        vm.trySendAction(SendMoneyAction.EnterManualIban(""))

        assertFalse(content(vm).fieldErrors.ibanInvalid)
    }
}
