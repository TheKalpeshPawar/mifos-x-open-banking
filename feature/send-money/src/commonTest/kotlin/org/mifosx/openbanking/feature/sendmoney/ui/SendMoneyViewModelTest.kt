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
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.feature.sendmoney.FakeAccountsOverviewRepository
import org.mifosx.openbanking.feature.sendmoney.FakeBeneficiariesRepository
import org.mifosx.openbanking.feature.sendmoney.FakePaymentInitiationRepository
import org.mifosx.openbanking.feature.sendmoney.SendMoneyFixtures
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val JAMESON_IDENTIFICATION = "40120965872310"

/**
 * Method names are camelCase because this source set also compiles for Kotlin/Native, whose frontend
 * rejects the punctuation a backticked prose name would carry.
 */
class SendMoneyViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        accounts: FakeAccountsOverviewRepository = FakeAccountsOverviewRepository(),
        beneficiaries: FakeBeneficiariesRepository = FakeBeneficiariesRepository(),
        payments: FakePaymentInitiationRepository = FakePaymentInitiationRepository(),
    ) = SendMoneyViewModel(accounts, beneficiaries, payments)

    private fun content(vm: SendMoneyViewModel): SendMoneyUiState.Content =
        assertIs<SendMoneyUiState.Content>(vm.stateFlow.value.uiState)

    /** Walks the form to the review step, which every submission test needs first. */
    private fun SendMoneyViewModel.completeForm(amountMinorUnits: String = "85000") {
        trySendAction(SendMoneyAction.SelectCreditor(SendMoneyFixtures.JAMESON_ID))
        trySendAction(SendMoneyAction.EnterAmount(amountMinorUnits))
        trySendAction(SendMoneyAction.ReviewPayment)
    }

    // region — loading & the recipient step

    @Test
    fun startsLoadingUntilTheAccountsArrive() = runTest {
        val accounts = FakeAccountsOverviewRepository(initial = ScreenState.Loading)

        val vm = viewModel(accounts = accounts)

        assertIs<SendMoneyUiState.Loading>(vm.stateFlow.value.uiState)
    }

    /** TC-SEND-001. */
    @Test
    fun rendersBothPayerRowsAndEveryPayeeRow() = runTest {
        val vm = viewModel()

        val state = content(vm)
        assertEquals(2, state.debtorRows.size)
        assertEquals(3, state.beneficiaries.size)
        assertEquals("Current account ·· 3349", state.debtorRows.first().headline)
        assertEquals("Jameson Lettings", state.beneficiaries.first().headline)
    }

    /** The picker lands usable rather than making the PSU choose an account they mostly always use. */
    @Test
    fun preselectsTheFirstAccount() = runTest {
        val vm = viewModel()

        assertEquals(SendMoneyFixtures.CURRENT_ACCOUNT_ID, content(vm).debtorAccountId)
    }

    /** A later refresh must not move a selection the PSU has made. */
    @Test
    fun aRefreshDoesNotOverrideTheChosenAccount() = runTest {
        val accounts = FakeAccountsOverviewRepository()
        val vm = viewModel(accounts = accounts)
        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.SAVINGS_ACCOUNT_ID))

        accounts.states.value = ScreenState.Content(SendMoneyFixtures.accounts(), DataFreshness.FRESH)

        assertEquals(SendMoneyFixtures.SAVINGS_ACCOUNT_ID, content(vm).debtorAccountId)
    }

    /** Counterparties are saved per account, so changing the payer changes who can be paid. */
    @Test
    fun changingThePayerRekeysThePayeeList() = runTest {
        val beneficiaries = FakeBeneficiariesRepository()
        val vm = viewModel(beneficiaries = beneficiaries)

        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.SAVINGS_ACCOUNT_ID))

        assertContentEquals(
            listOf(SendMoneyFixtures.CURRENT_ACCOUNT_ID, SendMoneyFixtures.SAVINGS_ACCOUNT_ID),
            beneficiaries.requestedAccountIds,
        )
    }

    @Test
    fun choosingAPayeeAdvancesToTheAmountStep() = runTest {
        val vm = viewModel()

        vm.trySendAction(SendMoneyAction.SelectCreditor(SendMoneyFixtures.JAMESON_ID))

        val state = content(vm)
        assertEquals(SendMoneyStep.Amount, state.step)
        assertEquals("Jameson Lettings", state.creditor?.name)
        assertEquals(JAMESON_IDENTIFICATION, state.creditor?.identification)
    }

    /** TC-SEND-013: an empty payee list is never a dead end — manual entry still works. */
    @Test
    fun anEmptyPayeeListStillAllowsAManualPayee() = runTest {
        val beneficiaries = FakeBeneficiariesRepository(
            initial = ScreenState.Content(emptyList(), DataFreshness.FRESH),
        )
        val vm = viewModel(beneficiaries = beneficiaries)

        assertFalse(content(vm).hasBeneficiaries)

        vm.trySendAction(SendMoneyAction.ShowManualCreditorEntry)
        vm.trySendAction(SendMoneyAction.EnterManualName("Ada Lovelace"))
        vm.trySendAction(SendMoneyAction.EnterManualSortCode("40-12-09"))
        vm.trySendAction(SendMoneyAction.EnterManualAccountNumber("65872310"))
        vm.trySendAction(SendMoneyAction.ConfirmManualCreditor)

        val state = content(vm)
        assertEquals(SendMoneyStep.Amount, state.step)
        assertEquals(JAMESON_IDENTIFICATION, state.creditor?.identification)
    }

    /** Separators are how a sort code is written; OBIE wants the digits. */
    @Test
    fun stripsSeparatorsFromAHandKeyedSortCode() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.ShowManualCreditorEntry)
        vm.trySendAction(SendMoneyAction.EnterManualSortCode("40-12-09"))
        vm.trySendAction(SendMoneyAction.EnterManualAccountNumber("65872310"))

        vm.trySendAction(SendMoneyAction.ConfirmManualCreditor)

        assertEquals(JAMESON_IDENTIFICATION, content(vm).creditor?.identification)
    }

    @Test
    fun refusesAManualPayeeWithTheWrongNumberOfDigits() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.ShowManualCreditorEntry)
        vm.trySendAction(SendMoneyAction.EnterManualSortCode("4012"))
        vm.trySendAction(SendMoneyAction.EnterManualAccountNumber("658"))

        vm.trySendAction(SendMoneyAction.ConfirmManualCreditor)

        val state = content(vm)
        assertTrue(state.fieldErrors.sortCodeInvalid)
        assertTrue(state.fieldErrors.accountNumberInvalid)
        assertEquals(SendMoneyStep.Recipient, state.step)
        assertNull(state.creditor)
    }

    /** Both fields can be wrong at once, which one message slot could not represent. */
    @Test
    fun reportsBothManualFieldsWhenBothAreWrong() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.ShowManualCreditorEntry)
        vm.trySendAction(SendMoneyAction.EnterManualSortCode("1"))
        vm.trySendAction(SendMoneyAction.EnterManualAccountNumber("2"))

        val errors = content(vm).fieldErrors
        assertTrue(errors.sortCodeInvalid)
        assertTrue(errors.accountNumberInvalid)
    }

    /** Flagging a length error before anything is typed reads as broken, not incomplete. */
    @Test
    fun staysQuietOnAnEmptyManualField() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.ShowManualCreditorEntry)

        vm.trySendAction(SendMoneyAction.EnterManualSortCode(""))

        assertFalse(content(vm).fieldErrors.sortCodeInvalid)
    }

    // endregion

    // region — the amount ladder

    /** TC-SEND-003: £850 against the £482.10 account. */
    @Test
    fun refusesAnAmountBeyondTheAvailableBalance() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.SAVINGS_ACCOUNT_ID))
        vm.trySendAction(SendMoneyAction.SelectCreditor(SendMoneyFixtures.JAMESON_ID))

        vm.trySendAction(SendMoneyAction.EnterAmount("85000"))

        val state = content(vm)
        assertEquals(SendMoneyAmountProblem.ExceedsAvailableBalance, state.amountProblem)
        assertFalse(state.canReview)
    }

    /** TC-SEND-004. */
    @Test
    fun refusesAnAmountOfZero() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectCreditor(SendMoneyFixtures.JAMESON_ID))

        vm.trySendAction(SendMoneyAction.EnterAmount("0"))

        assertEquals(SendMoneyAmountProblem.NotPositive, content(vm).amountProblem)
        assertFalse(content(vm).canReview)
    }

    @Test
    fun refusesAnAmountThatIsNotANumber() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectCreditor(SendMoneyFixtures.JAMESON_ID))

        vm.trySendAction(SendMoneyAction.EnterAmount("eight hundred"))

        assertEquals(SendMoneyAmountProblem.NotANumber, content(vm).amountProblem)
    }

    @Test
    fun acceptsAnAmountWithinTheBalance() = runTest {
        val vm = viewModel()
        vm.trySendAction(SendMoneyAction.SelectCreditor(SendMoneyFixtures.JAMESON_ID))

        vm.trySendAction(SendMoneyAction.EnterAmount("85000"))

        val state = content(vm)
        assertNull(state.amountProblem)
        assertTrue(state.canReview)
        assertEquals("£850.00", state.amountLabel)
    }

    // endregion

    // region — staging, submitting, and the two invariants

    @Test
    fun stagingMovesToSubmittingAndLaunchesTheAuthorisation() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val state = assertIs<SendMoneyUiState.Submitting>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyStage.AwaitingAuthorisation, state.stage)
        assertEquals(SendMoneyFixtures.CONSENT_ID, vm.stateFlow.value.consentId)
        assertEquals(1, payments.stagedDrafts.size)
    }

    /** TC-SEND-006: paying an account the PSU also owns is a transfer to self. */
    @Test
    fun marksAPaymentToAnOwnAccountAsATransferToSelf() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.trySendAction(SendMoneyAction.ShowManualCreditorEntry)
        vm.trySendAction(SendMoneyAction.EnterManualSortCode("801225"))
        vm.trySendAction(SendMoneyAction.EnterManualAccountNumber("90953695"))
        vm.trySendAction(SendMoneyAction.ConfirmManualCreditor)
        vm.trySendAction(SendMoneyAction.EnterAmount("1000"))
        vm.trySendAction(SendMoneyAction.ReviewPayment)

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        assertTrue(payments.stagedDrafts.single().creditor.isOwnAccount)
    }

    @Test
    fun payingAThirdPartyIsNotMarkedAsATransferToSelf() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        assertFalse(payments.stagedDrafts.single().creditor.isOwnAccount)
    }

    @Test
    fun aSuccessfulSubmissionReportsThePaymentId() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        vm.trySendAction(SendMoneyAction.SubmitPayment)

        val state = assertIs<SendMoneyUiState.Success>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyFixtures.PAYMENT_ID, state.paymentId)
        assertEquals("£850.00", state.amountLabel)
        assertEquals("Jameson Lettings", state.creditorName)
    }

    /**
     * TC-SEND-010, and the single most important assertion in this suite.
     *
     * A retry that mints a fresh key is not a retry — it is a second payment instruction the bank
     * cannot recognise as a duplicate. The draft must be identical too, or the resubmitted
     * `Initiation` diverges from the staged one and the bank refuses it with `U008`.
     */
    @Test
    fun aRetryReusesTheSameIdempotencyKeyAndDraft() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)
        payments.submitReturns(NetworkResult.Error(NetworkError.Network(cause = RuntimeException("offline"))))
        vm.trySendAction(SendMoneyAction.SubmitPayment)

        payments.submitReturns(NetworkResult.Success(SendMoneyFixtures.receipt()))
        vm.trySendAction(SendMoneyAction.RetrySubmit)

        assertEquals(2, payments.submittedDrafts.size)
        val first = payments.submittedDrafts[0]
        val second = payments.submittedDrafts[1]
        assertEquals(first.idempotencyKey, second.idempotencyKey)
        assertEquals(first, second)
        assertEquals(first.idempotencyKey, vm.stateFlow.value.idempotencyKey)
    }

    /** Staging once and submitting must use one instruction, not two derivations of it. */
    @Test
    fun submitsTheDraftThatWasStaged() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        vm.trySendAction(SendMoneyAction.SubmitPayment)

        assertEquals(payments.stagedDrafts.single(), payments.submittedDrafts.single())
        assertEquals(SendMoneyFixtures.CONSENT_ID, payments.submittedConsentIds.single())
    }

    /** The OBIE cap is 40 characters. */
    @Test
    fun mintsAnIdempotencyKeyWithinTheObieLimit() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val key = payments.stagedDrafts.single().idempotencyKey
        assertTrue(key.isNotBlank())
        assertTrue(key.length <= 40, "idempotency key was ${key.length} characters")
    }

    /** TC-SEND-011: a negative funds check stops the payment before it is sent. */
    @Test
    fun aNegativeFundsCheckStopsTheSubmission() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.fundsReturn(NetworkResult.Success(false))
        val vm = viewModel(payments = payments)
        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        vm.trySendAction(SendMoneyAction.SubmitPayment)

        val state = assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyErrorKind.InsufficientFunds, state.kind)
        assertTrue(payments.submittedDrafts.isEmpty())
    }

    // endregion

    // region — failures and recovery

    /** TC-SEND-007: nothing the PSU can do, so no CTA and a reference to quote. */
    @Test
    fun aSignatureFailureCarriesASupportReferenceAndNoRetry() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.stageReturns(
            NetworkResult.Error(
                NetworkError.Client.BadRequest(
                    """{"Id":"${SendMoneyFixtures.SUPPORT_REFERENCE}","Errors":[{"ErrorCode":"U019"}]}""",
                ),
            ),
        )
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val state = assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyErrorKind.SignatureMissing, state.kind)
        assertEquals(SendMoneyFixtures.SUPPORT_REFERENCE, state.supportReference)
        assertFalse(state.kind.isRetryable)
    }

    /** TC-SEND-008. */
    @Test
    fun anUnauthorisedConsentAsksForReauthorisation() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.stageReturns(
            NetworkResult.Error(NetworkError.Client.BadRequest("""{"Errors":[{"ErrorCode":"U009"}]}""")),
        )
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val state = assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyErrorKind.ConsentNotAuthorised, state.kind)
        assertTrue(state.kind.needsReauthorisation)
        assertFalse(state.kind.isRetryable)
    }

    /** TC-SEND-009: editing the amount keeps the payer and payee already chosen. */
    @Test
    fun editingTheAmountReturnsToTheAmountStepWithSelectionsIntact() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.stageReturns(
            NetworkResult.Error(NetworkError.Client.BadRequest("""{"Errors":[{"ErrorCode":"U014"}]}""")),
        )
        val vm = viewModel(payments = payments)
        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)
        assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)

        vm.trySendAction(SendMoneyAction.BackStep)

        val state = content(vm)
        assertEquals(SendMoneyStep.Amount, state.step)
        assertEquals("Jameson Lettings", state.creditor?.name)
        assertEquals(SendMoneyFixtures.CURRENT_ACCOUNT_ID, state.debtorAccountId)
    }

    /**
     * Changing the amount makes it a different instruction, so the next attempt must be staged
     * afresh rather than replayed under the key that identified the old one.
     */
    @Test
    fun editingTheAmountDiscardsTheStagedDraftAndKey() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        vm.trySendAction(SendMoneyAction.BackStep)

        assertEquals("", vm.stateFlow.value.idempotencyKey)
        assertNull(vm.stateFlow.value.draft)
        assertNull(vm.stateFlow.value.consentId)
    }

    @Test
    fun aRevokedConsentPointsAtTheConsentScreens() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.stageReturns(NetworkResult.Error(NetworkError.Client.Forbidden(null)))
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val state = assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyErrorKind.ConsentRevoked, state.kind)
        assertFalse(state.kind.isRetryable)
    }

    @Test
    fun aTransportFailureOffersRetry() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.stageReturns(NetworkResult.Error(NetworkError.Network(cause = RuntimeException("offline"))))
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val state = assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyErrorKind.NetworkError, state.kind)
        assertTrue(state.kind.isRetryable)
    }

    /** Nothing was sent, so there is nothing to revoke — just clear and start over. */
    @Test
    fun cancellingReturnsToTheRecipientStepAndClearsTheDraft() = runTest {
        val vm = viewModel()
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.CancelPayment)

        val state = content(vm)
        assertEquals(SendMoneyStep.Recipient, state.step)
        assertNull(state.creditor)
        assertNull(vm.stateFlow.value.draft)
        assertEquals("", vm.stateFlow.value.idempotencyKey)
    }

    @Test
    fun anAccountsFailureSurfacesAsAScreenError() = runTest {
        val accounts = FakeAccountsOverviewRepository(
            initial = ScreenState.Error(RemoteException(NetworkError.Client.Forbidden(null))),
        )

        val vm = viewModel(accounts = accounts)

        val state = assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyErrorKind.ConsentRevoked, state.kind)
    }

    @Test
    fun retryLoadRefreshesTheAccounts() = runTest {
        val accounts = FakeAccountsOverviewRepository()
        val vm = viewModel(accounts = accounts)

        vm.trySendAction(SendMoneyAction.RetryLoad)

        assertEquals(1, accounts.refreshCount)
    }

    /** Nothing to submit without a staged consent, and asking anyway must not throw. */
    @Test
    fun submittingWithoutAStagedConsentDoesNothing() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.SubmitPayment)

        assertTrue(payments.submittedDrafts.isEmpty())
        assertNotNull(vm.stateFlow.value.uiState)
    }

    // endregion
}
