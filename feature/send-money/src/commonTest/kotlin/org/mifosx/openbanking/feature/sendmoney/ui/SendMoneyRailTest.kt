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
     * The transfer currency defaults to GBP, which the international rail never offers.
     *
     * Left alone it would stage `CurrencyOfTransfer: GBP` with nothing on screen having said so, and
     * Global Money refuses anything outside USD and EUR with `U002`.
     */
    @Test
    fun switchingToInternationalMovesTheTransferCurrencyOffSterling() = runTest {
        val vm = viewModel()
        assertEquals("GBP", content(vm).currencyOfTransfer)

        vm.trySendAction(SendMoneyAction.SelectRail(PaymentRail.International))

        val state = content(vm)
        assertEquals("USD", state.currencyOfTransfer)
        assertContentEquals(listOf("USD", "EUR"), state.transferCurrencies)
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
