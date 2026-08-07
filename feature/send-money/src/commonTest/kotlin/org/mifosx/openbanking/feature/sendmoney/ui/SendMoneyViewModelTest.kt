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
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint
import org.mifosx.openbanking.feature.sendmoney.FakeAccountCapabilityRegistry
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

    private val registry = FakeAccountCapabilityRegistry()

    private fun viewModel(
        accounts: FakeAccountsOverviewRepository = FakeAccountsOverviewRepository(),
        beneficiaries: FakeBeneficiariesRepository = FakeBeneficiariesRepository(),
        payments: FakePaymentInitiationRepository = FakePaymentInitiationRepository(),
    ) = SendMoneyViewModel(accounts, beneficiaries, payments, registry)

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

    /**
     * TC-SEND-001. Two of the four fixture accounts are payable: the credit card is dropped on its
     * subtype and the Global Money wallet on its description, both before the bank is asked.
     */
    @Test
    fun rendersEveryPayablePayerRowAndEveryPayeeRow() = runTest {
        val vm = viewModel()

        val state = content(vm)
        assertEquals(2, state.debtorRows.size)
        assertFalse(SendMoneyFixtures.CREDIT_CARD_ID in state.debtorRows.map { it.id })
        assertFalse(SendMoneyFixtures.GLOBAL_MONEY_ID in state.debtorRows.map { it.id })
        assertEquals(3, state.beneficiaries.size)
        assertEquals("Jameson Lettings", state.beneficiaries.first().headline)
    }

    /**
     * The regression guard for the blank-row defect.
     *
     * HSBC leaves `Nickname` blank on most accounts, so a row must carry the fields
     * `accountDisplayName` needs to derive "Current account ·· 3349". Reading `nickname` as the
     * headline rendered an empty name and an empty avatar on every live account — and the original
     * fixtures hid it, because they gave every account a nickname the real ones do not have.
     */
    @Test
    fun anAccountWithNoNicknameStillCarriesEnoughToNameItself() = runTest {
        val accounts = FakeAccountsOverviewRepository(
            initial = ScreenState.Content(
                listOf(
                    AccountWithBalance(
                        account = SendMoneyFixtures.currentAccount().copy(nickname = ""),
                        balance = null,
                    ),
                ),
                DataFreshness.FRESH,
            ),
        )

        val row = content(viewModel(accounts = accounts)).debtorRows.single()

        assertEquals("CurrentAccount", row.accountSubType)
        assertEquals("10203349", row.accountNumber)
        assertEquals("80200110203349", row.rawIdentification)
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

    /**
     * A credit card cannot fund a domestic payment and must not be offered as one.
     *
     * Verified live: staging with the card as `DebtorAccount` is refused with `400 U021` — "the
     * DebtorAccount.Identification value is incorrect for SchemeName
     * 'UK.OBIE.SortCodeAccountNumber'". A card has no sort code and account number, so the app sent
     * the masked PAN HSBC gave it and the bank rejected the field. Filtering beats letting someone
     * fill in a payee and an amount before meeting a guaranteed refusal.
     */
    @Test
    fun aCreditCardIsNotOfferedAsAPayer() = runTest {
        val vm = viewModel()

        val ids = content(vm).debtorRows.map { it.id }

        assertFalse(SendMoneyFixtures.CREDIT_CARD_ID in ids)
        assertTrue(SendMoneyFixtures.CURRENT_ACCOUNT_ID in ids)
        assertTrue(SendMoneyFixtures.SAVINGS_ACCOUNT_ID in ids)
    }

    /**
     * The Global Money wallet is excluded BEFORE the bank is asked.
     *
     * It reports `AccountTypeCode: CACC`, identical to a current account, so the only thing that
     * gives it away is the free-text `Description`. That used to be passed to the resolver as an
     * empty string, so every wallet was offered and removed only once the bank refused it with
     * `U002` — and since the capability registry is in memory, it returned on the next launch to
     * fail the same way. This is the regression test for that.
     */
    @Test
    fun aGlobalMoneyWalletIsNotOfferedAsAPayer() = runTest {
        val vm = viewModel()

        val ids = content(vm).debtorRows.map { it.id }

        assertFalse(SendMoneyFixtures.GLOBAL_MONEY_ID in ids)
        assertTrue(SendMoneyFixtures.CURRENT_ACCOUNT_ID in ids)
    }

    /**
     * A wallet the bank did not describe is indistinguishable again, and falls back to the refusal.
     *
     * Proves the description is what does the work above, rather than some other property of the
     * fixture, and that removing the prediction does not remove the safety net.
     */
    @Test
    fun aWalletWithNoDescriptionFallsBackToTheBanksRefusal() = runTest {
        val undisclosed = SendMoneyFixtures.accounts().map { row ->
            if (row.account.accountId == SendMoneyFixtures.GLOBAL_MONEY_ID) {
                row.copy(account = row.account.copy(description = ""))
            } else {
                row
            }
        }
        val vm = viewModel(
            accounts = FakeAccountsOverviewRepository(
                ScreenState.Content(undisclosed, DataFreshness.FRESH),
            ),
        )
        assertTrue(SendMoneyFixtures.GLOBAL_MONEY_ID in content(vm).debtorRows.map { it.id })

        registry.markUnsupported(SendMoneyFixtures.GLOBAL_MONEY_ID, AccountEndpoint.PaymentDebtor)

        assertFalse(SendMoneyFixtures.GLOBAL_MONEY_ID in content(vm).debtorRows.map { it.id })
    }

    /** The registry still removes any payer the bank refuses, matrix prediction or not. */
    @Test
    fun aRefusedPayerDisappearsFromThePicker() = runTest {
        val vm = viewModel()
        assertTrue(SendMoneyFixtures.SAVINGS_ACCOUNT_ID in content(vm).debtorRows.map { it.id })

        registry.markUnsupported(SendMoneyFixtures.SAVINGS_ACCOUNT_ID, AccountEndpoint.PaymentDebtor)

        val ids = content(vm).debtorRows.map { it.id }
        assertFalse(SendMoneyFixtures.SAVINGS_ACCOUNT_ID in ids)
        assertTrue(SendMoneyFixtures.CURRENT_ACCOUNT_ID in ids)
    }

    /** A refusal about another endpoint must not cost the account its payer role. */
    @Test
    fun anUnrelatedRefusalDoesNotRemoveAPayer() = runTest {
        val vm = viewModel()

        registry.markUnsupported(SendMoneyFixtures.SAVINGS_ACCOUNT_ID, AccountEndpoint.DirectDebits)

        assertTrue(SendMoneyFixtures.SAVINGS_ACCOUNT_ID in content(vm).debtorRows.map { it.id })
    }

    /**
     * A payer refusal is not a connection problem and not a typo. There is nothing to check and
     * nothing to retry — the only useful move is a different account.
     */
    @Test
    fun aRefusedPayerIsReportedAsSuchAndOffersAWayOut() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.stageReturns(
            NetworkResult.Error(
                NetworkError.Client.BadRequest(
                    """{"Errors":[{"ErrorCode":"U002","Path":"Data.Initiation.DebtorAccount.Identification"}]}""",
                ),
            ),
        )
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val state = assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyErrorKind.PayerNotSupported, state.kind)
        assertFalse(state.kind.isRetryable)
    }

    /** Changing payer returns to the payer step, drops the refused selection, keeps the payee. */
    @Test
    fun changingPayerReturnsToTheRecipientStepWithThePayeeIntact() = runTest {
        val vm = viewModel()
        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        vm.trySendAction(SendMoneyAction.ChangePayer)

        val state = content(vm)
        assertEquals(SendMoneyStep.Recipient, state.step)
        assertNull(state.debtorAccountId)
        assertNotNull(state.creditor)
        assertNull(vm.stateFlow.value.draft)
    }

    /**
     * The draft is staged, stored, and left for the returning leg to submit. This screen must never
     * send the payment itself — the browser hop destroys it, so a submission from here would be
     * running in a ViewModel that is about to cease existing.
     */
    @Test
    fun stagingDoesNotSubmitThePayment() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        assertEquals(1, payments.stagedDrafts.size)
        assertTrue(payments.submittedDrafts.isEmpty())
        assertTrue(payments.fundsChecks.isEmpty())
    }

    /** The OBIE cap is 40 characters. */
    @Test
    fun mintsAnIdempotencyKeyWithinTheObieLimit() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val draft = payments.stagedDrafts.single()
        listOf(draft.consentIdempotencyKey, draft.paymentIdempotencyKey).forEach { key ->
            assertTrue(key.isNotBlank())
            assertTrue(key.length <= 40, "idempotency key was ${key.length} characters")
        }
    }

    /**
     * The consent and submission bodies differ — only the latter carries `Data.ConsentId` — and the
     * Read/Write profile answers a repeated key over a changed body with `400 U029`. So the two
     * calls must not share one key, however tempting the symmetry.
     */
    @Test
    fun theConsentAndPaymentPostsCarryDifferentIdempotencyKeys() = runTest {
        val payments = FakePaymentInitiationRepository()
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val draft = payments.stagedDrafts.single()
        assertTrue(draft.consentIdempotencyKey != draft.paymentIdempotencyKey)
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

    /**
     * `U021` names a field the bank would not accept, so it must read as a rejected detail — not as
     * a connection problem with a Retry that can only fail again.
     *
     * Observed live: sending a credit card as `DebtorAccount` returned `400 U021`, and the app said
     * "Check your connection and try again". The code fell through to `NetworkError` because the
     * classifier did not know it.
     */
    @Test
    fun aRejectedFieldIsNotReportedAsANetworkProblem() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.stageReturns(
            NetworkResult.Error(NetworkError.Client.BadRequest("""{"Errors":[{"ErrorCode":"U021"}]}""")),
        )
        val vm = viewModel(payments = payments)
        vm.completeForm()

        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        val state = assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyErrorKind.InvalidField, state.kind)
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

    /**
     * Editing from Review keeps the payer and payee but drops the draft: changing the amount makes
     * it a different instruction, which must be staged under new keys rather than replayed.
     */
    @Test
    fun editingFromReviewReturnsToTheAmountStepAndClearsTheDraft() = runTest {
        val vm = viewModel()
        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ReviewPayment)

        vm.trySendAction(SendMoneyAction.BackStep)

        val state = content(vm)
        assertEquals(SendMoneyStep.Amount, state.step)
        assertNotNull(state.creditor)
        assertNull(vm.stateFlow.value.draft)
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

    /**
     * A staging failure leaves no consent, so the only sensible retry is to stage again — which
     * mints a fresh consent under fresh keys, because the earlier attempt never produced one for the
     * bank to recognise as a duplicate.
     */
    @Test
    fun retryingAfterAStagingFailureStagesAgain() = runTest {
        val payments = FakePaymentInitiationRepository()
        payments.stageReturns(
            NetworkResult.Error(NetworkError.Network(cause = RuntimeException("offline"))),
        )
        val vm = viewModel(payments = payments)
        vm.completeForm()
        vm.trySendAction(SendMoneyAction.ConfirmAndStageConsent)

        payments.stageReturns(NetworkResult.Success(SendMoneyFixtures.stagedConsent()))
        vm.trySendAction(SendMoneyAction.RetryStaging)

        assertEquals(2, payments.stagedDrafts.size)
        assertNotNull(vm.stateFlow.value.consentId)
    }

    // endregion
}
