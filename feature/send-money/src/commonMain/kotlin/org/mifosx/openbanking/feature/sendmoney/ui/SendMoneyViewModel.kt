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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.common.formatMinorUnits
import org.mifosx.openbanking.core.common.formatSortCode
import org.mifosx.openbanking.core.common.parseMinorUnits
import org.mifosx.openbanking.core.data.banking.AccountsOverviewRepository
import org.mifosx.openbanking.core.data.banking.BeneficiariesRepository
import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.data.util.toThrowable
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.banking.payment.CreditorSelection
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkResult
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SORT_CODE_DIGITS = 6
private const val ACCOUNT_NUMBER_DIGITS = 8
private const val INSTRUCTION_ID_PREFIX = "MFX"
private const val END_TO_END_ID_PREFIX = "E2E"

/**
 * Drives the payment journey.
 *
 * Two things here are load-bearing and neither is obvious from the shape of the code:
 *
 * The **idempotency key and the draft are minted exactly once**, in [confirmAndStageConsent], and
 * every later call reuses them. A retry that mints a fresh key is not a retry — the bank has no way
 * to recognise it as a duplicate and pays twice. For the same reason the draft is stored rather than
 * rebuilt: the submitted `Initiation` must be byte-identical to the staged one.
 *
 * The **form survives stream emissions**. Loaded data ([accountsScreen], [beneficiariesScreen]) and
 * entered data ([form]) are separate flows combined into one `Content`, so an account refresh
 * arriving mid-form cannot discard what the PSU has already typed.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
@Suppress("TooManyFunctions")
class SendMoneyViewModel(
    private val accountsOverviewRepository: AccountsOverviewRepository,
    private val beneficiariesRepository: BeneficiariesRepository,
    private val paymentInitiationRepository: PaymentInitiationRepository,
) : BaseViewModel<SendMoneyState, SendMoneyEvent, SendMoneyAction>(initialState = SendMoneyState()) {

    /** Everything the PSU has entered. Held apart from loaded data so a refresh cannot clear it. */
    private data class Form(
        val step: SendMoneyStep = SendMoneyStep.Recipient,
        val debtorAccountId: String? = null,
        val creditor: CreditorSelection? = null,
        val manualEntryVisible: Boolean = false,
        val manualSortCode: String = "",
        val manualAccountNumber: String = "",
        val manualName: String = "",
        val amountMinorUnits: String = "",
        val reference: String = "",
        val amountProblem: SendMoneyAmountProblem? = null,
        val fieldErrors: SendMoneyFieldErrors = SendMoneyFieldErrors(),
    )

    /** Which of the four screen states is showing. The form only renders under [Phase.Form]. */
    private sealed interface Phase {
        data object Form : Phase
        data class Submitting(val stage: SendMoneyStage, val consentId: String?) : Phase
        data class Success(val receipt: PaymentReceipt) : Phase
        data class Error(val kind: SendMoneyErrorKind, val supportReference: String?) : Phase
    }

    private val accountsScreen =
        MutableStateFlow<ScreenState<List<AccountWithBalance>>>(ScreenState.Loading)
    private val beneficiariesScreen =
        MutableStateFlow<ScreenState<List<BeneficiaryItem>>>(ScreenState.Loading)
    private val form = MutableStateFlow(Form())
    private val phase = MutableStateFlow<Phase>(Phase.Form)
    private val selectedAccountId = MutableStateFlow("")

    private val beneficiariesStream: Flow<ScreenState<List<BeneficiaryItem>>> =
        selectedAccountId
            .filter { it.isNotBlank() }
            .distinctUntilChanged()
            .flatMapLatest { accountId ->
                beneficiariesRepository.beneficiariesStream(accountId, viewModelScope).state
            }

    init {
        accountsOverviewRepository.overviewState(viewModelScope)
            .onEach { screen ->
                accountsScreen.value = screen
                seedSelection(screen)
            }
            .launchIn(viewModelScope)

        beneficiariesStream
            .onEach { beneficiariesScreen.value = it }
            .launchIn(viewModelScope)

        combine(accountsScreen, beneficiariesScreen, form, phase) { accounts, payees, entered, current ->
            render(accounts, payees, entered, current)
        }
            .onEach { rendered -> updateState { copy(uiState = rendered) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SendMoneyAction) {
        when (action) {
            is SendMoneyAction.SelectDebtorAccount -> selectDebtor(action.accountId)
            is SendMoneyAction.SelectCreditor -> selectCreditor(action.beneficiaryId)
            SendMoneyAction.ShowManualCreditorEntry -> form.value = form.value.copy(manualEntryVisible = true)
            is SendMoneyAction.EnterManualSortCode -> enterSortCode(action.sortCode)
            is SendMoneyAction.EnterManualAccountNumber -> enterAccountNumber(action.accountNumber)
            is SendMoneyAction.EnterManualName -> form.value = form.value.copy(manualName = action.name)
            SendMoneyAction.ConfirmManualCreditor -> confirmManualCreditor()
            is SendMoneyAction.EnterAmount -> enterAmount(action.minorUnits)
            is SendMoneyAction.EnterReference -> enterReference(action.reference)
            SendMoneyAction.ReviewPayment -> form.value = form.value.copy(step = SendMoneyStep.Review)
            SendMoneyAction.ConfirmAndStageConsent -> confirmAndStageConsent()
            SendMoneyAction.SubmitPayment -> submitPayment()
            SendMoneyAction.RetrySubmit -> submitPayment()
            SendMoneyAction.CancelPayment -> cancelPayment()
            SendMoneyAction.BackStep -> backStep()
            SendMoneyAction.RetryLoad -> accountsOverviewRepository.refresh()
        }
    }

    /**
     * Pre-selects the first account so the form opens usable, matching the picker's landing state.
     * Only ever seeds — a later refresh must not move a selection the PSU has made.
     */
    private fun seedSelection(screen: ScreenState<List<AccountWithBalance>>) {
        if (form.value.debtorAccountId != null) return
        val first = (screen as? ScreenState.Content)?.data?.firstOrNull()?.account ?: return
        form.value = form.value.copy(debtorAccountId = first.accountId)
        selectedAccountId.value = first.accountId
    }

    /** Changing the payer re-keys the payee list: counterparties are saved per account, not per customer. */
    private fun selectDebtor(accountId: String) {
        selectedAccountId.value = accountId
        form.value = form.value.copy(debtorAccountId = accountId, creditor = null)
    }

    private fun selectCreditor(beneficiaryId: String) {
        val payee = beneficiaries().firstOrNull { it.beneficiaryId == beneficiaryId } ?: return
        form.value = form.value.copy(
            creditor = CreditorSelection(
                name = payee.creditorName,
                scheme = payee.scheme,
                identification = payee.identification,
                beneficiaryId = payee.beneficiaryId,
                isOwnAccount = isOwnAccount(payee.identification),
            ),
            reference = payee.reference,
            step = SendMoneyStep.Amount,
            manualEntryVisible = false,
        )
    }

    /**
     * Validates as the PSU types, but stays quiet on an empty field: flagging a length error before
     * anyone has typed anything reads as the form being broken rather than incomplete.
     */
    private fun enterSortCode(raw: String) {
        val digits = raw.filter(Char::isDigit)
        form.value = form.value.copy(
            manualSortCode = raw,
            fieldErrors = form.value.fieldErrors.copy(
                sortCodeInvalid = raw.isNotEmpty() && digits.length != SORT_CODE_DIGITS,
            ),
        )
    }

    private fun enterAccountNumber(raw: String) {
        val digits = raw.filter(Char::isDigit)
        form.value = form.value.copy(
            manualAccountNumber = raw,
            fieldErrors = form.value.fieldErrors.copy(
                accountNumberInvalid = raw.isNotEmpty() && digits.length != ACCOUNT_NUMBER_DIGITS,
            ),
        )
    }

    /**
     * Accepts a hand-keyed payee once both fields hold the right number of digits.
     *
     * Separators are stripped before validation and before the identification is built: OBIE wants
     * the fourteen digits unpunctuated, while `40-12-09` is how a sort code is written down.
     */
    private fun confirmManualCreditor() {
        val current = form.value
        val sortCode = current.manualSortCode.filter(Char::isDigit)
        val accountNumber = current.manualAccountNumber.filter(Char::isDigit)
        val errors = SendMoneyFieldErrors(
            sortCodeInvalid = sortCode.length != SORT_CODE_DIGITS,
            accountNumberInvalid = accountNumber.length != ACCOUNT_NUMBER_DIGITS,
        )
        if (!errors.isEmpty) {
            form.value = current.copy(fieldErrors = errors)
            return
        }
        val identification = sortCode + accountNumber
        form.value = current.copy(
            creditor = CreditorSelection(
                name = current.manualName,
                scheme = BeneficiaryScheme.SortCode,
                identification = identification,
                isOwnAccount = isOwnAccount(identification),
            ),
            fieldErrors = SendMoneyFieldErrors(),
            step = SendMoneyStep.Amount,
        )
    }

    /**
     * The validation ladder, in order: parseable, positive, within the account's available balance.
     *
     * The balance rung is advisory only — it never replaces the bank's funds confirmation, which can
     * refuse a payment this comparison allows.
     */
    private fun enterAmount(minorUnits: String) {
        val available = availableBalanceMinorUnits()
        val parsed = minorUnits.takeIf { it.isNotBlank() }?.toLongOrNull()
        val problem = when {
            minorUnits.isBlank() -> null
            parsed == null -> SendMoneyAmountProblem.NotANumber
            parsed <= 0L -> SendMoneyAmountProblem.NotPositive
            available != null && parsed > available -> SendMoneyAmountProblem.ExceedsAvailableBalance
            else -> null
        }
        form.value = form.value.copy(amountMinorUnits = minorUnits, amountProblem = problem)
    }

    private fun enterReference(reference: String) {
        form.value = form.value.copy(reference = reference)
    }

    /**
     * Fixes the instruction and hands it to the bank.
     *
     * This is where the idempotency key and both OBIE identifiers are minted, once. Everything after
     * this point — submission, and any retry — reuses the draft built here rather than deriving a
     * new one.
     */
    private fun confirmAndStageConsent() {
        val draft = buildDraft() ?: return
        phase.value = Phase.Submitting(SendMoneyStage.StagingConsent, consentId = null)
        updateState { copy(idempotencyKey = draft.idempotencyKey, draft = draft) }

        viewModelScope.launch {
            when (val result = paymentInitiationRepository.stagePayment(draft)) {
                is NetworkResult.Success -> {
                    val staged = result.data
                    updateState { copy(consentId = staged.consentId) }
                    phase.value = Phase.Submitting(SendMoneyStage.AwaitingAuthorisation, staged.consentId)
                    sendEvent(SendMoneyEvent.LaunchAuthorisation(staged.authorizationUrl))
                }

                is NetworkResult.Error -> fail(result.error.toThrowable())
            }
        }
    }

    /**
     * Confirms funds, then submits.
     *
     * A negative funds check stops here: the protocol calls the confirmation optional, but once made
     * its answer is binding, and submitting anyway would be knowingly sending a payment the bank has
     * just said it cannot cover.
     */
    private fun submitPayment() {
        val draft = state.draft ?: return
        val consentId = state.consentId ?: return
        phase.value = Phase.Submitting(SendMoneyStage.SubmittingPayment, consentId)

        viewModelScope.launch {
            when (val funds = paymentInitiationRepository.confirmFunds(consentId)) {
                is NetworkResult.Success ->
                    if (funds.data) {
                        submitConfirmed(draft, consentId)
                    } else {
                        phase.value = Phase.Error(SendMoneyErrorKind.InsufficientFunds, supportReference = null)
                    }

                is NetworkResult.Error -> fail(funds.error.toThrowable())
            }
        }
    }

    private suspend fun submitConfirmed(draft: PaymentDraft, consentId: String) {
        when (val result = paymentInitiationRepository.submitPayment(draft, consentId)) {
            is NetworkResult.Success -> {
                phase.value = Phase.Success(result.data)
                sendEvent(SendMoneyEvent.PaymentSucceeded(result.data.domesticPaymentId))
            }

            is NetworkResult.Error -> fail(result.error.toThrowable())
        }
    }

    private fun fail(throwable: Throwable) {
        phase.value = Phase.Error(
            kind = classifySendMoneyError(throwable),
            supportReference = supportReferenceOf(throwable),
        )
    }

    /** Abandons before anything was sent, so there is nothing to revoke — just clear and restart. */
    private fun cancelPayment() {
        form.value = Form(debtorAccountId = form.value.debtorAccountId)
        phase.value = Phase.Form
        updateState { copy(idempotencyKey = "", draft = null, consentId = null) }
    }

    /**
     * Returns to the amount step from a failure, keeping the payer and payee already chosen.
     *
     * The draft is dropped deliberately: editing the amount changes the instruction, so the next
     * attempt is a different payment and must be staged under a new key rather than replayed.
     */
    private fun backStep() {
        form.value = form.value.copy(step = SendMoneyStep.Amount)
        phase.value = Phase.Form
        updateState { copy(idempotencyKey = "", draft = null, consentId = null) }
    }

    /**
     * Four symmetric guards rather than tangled control flow: the draft can only be built once the
     * payer, payee and amount are all present, and each absence means the same thing — not ready.
     */
    @Suppress("ReturnCount")
    private fun buildDraft(): PaymentDraft? {
        val current = form.value
        val account = debtorAccount() ?: return null
        val creditor = current.creditor ?: return null
        val amount = current.amountMinorUnits.toLongOrNull() ?: return null
        val hex = Uuid.generateV4().toHexString()
        return PaymentDraft(
            debtorAccount = account,
            creditor = creditor,
            amountMinorUnits = amount,
            currency = account.currency,
            reference = current.reference.takeIf { it.isNotBlank() },
            instructionIdentification = INSTRUCTION_ID_PREFIX + hex,
            endToEndIdentification = END_TO_END_ID_PREFIX + hex,
            idempotencyKey = Uuid.generateV4().toString(),
        )
    }

    private fun render(
        accounts: ScreenState<List<AccountWithBalance>>,
        payees: ScreenState<List<BeneficiaryItem>>,
        entered: Form,
        current: Phase,
    ): SendMoneyUiState = when (current) {
        is Phase.Submitting -> SendMoneyUiState.Submitting(
            stage = current.stage,
            amountLabel = amountLabel(entered),
            creditorName = entered.creditor?.name.orEmpty(),
            consentId = current.consentId,
        )

        is Phase.Success -> SendMoneyUiState.Success(
            paymentId = current.receipt.domesticPaymentId,
            statusLabel = current.receipt.status.name,
            amountLabel = current.receipt.amountLabel,
            creditorName = current.receipt.creditorName,
        )

        is Phase.Error -> SendMoneyUiState.Error(current.kind, current.supportReference)
        Phase.Form -> renderForm(accounts, payees, entered)
    }

    private fun renderForm(
        accounts: ScreenState<List<AccountWithBalance>>,
        payees: ScreenState<List<BeneficiaryItem>>,
        entered: Form,
    ): SendMoneyUiState = when (accounts) {
        is ScreenState.Content -> content(accounts.data, payees, entered)
        is ScreenState.Error -> SendMoneyUiState.Error(classifySendMoneyError(accounts.error), null)
        is ScreenState.NoNetwork -> SendMoneyUiState.Error(SendMoneyErrorKind.NetworkError, null)
        ScreenState.Unauthenticated -> SendMoneyUiState.Error(SendMoneyErrorKind.TokenExpired, null)
        ScreenState.Empty, ScreenState.Loading -> SendMoneyUiState.Loading
    }

    private fun content(
        accounts: List<AccountWithBalance>,
        payees: ScreenState<List<BeneficiaryItem>>,
        entered: Form,
    ): SendMoneyUiState.Content {
        val payeeList = (payees as? ScreenState.Content)?.data.orEmpty()
        val selected = accounts.firstOrNull { it.account.accountId == entered.debtorAccountId }
        return SendMoneyUiState.Content(
            step = entered.step,
            debtorAccounts = accounts.map { it.account },
            debtorRows = accounts.map { it.toPickerRow() },
            beneficiaries = payeeList.map { it.toPickerRow() },
            debtorAccountId = entered.debtorAccountId,
            creditor = entered.creditor,
            creditorLabel = entered.creditor?.name.orEmpty(),
            creditorSupporting = entered.creditor?.let { schemeLabel(it) }.orEmpty(),
            debtorAccountLabel = selected?.account?.nickname.orEmpty(),
            manualEntryVisible = entered.manualEntryVisible,
            manualSortCode = entered.manualSortCode,
            manualAccountNumber = entered.manualAccountNumber,
            manualName = entered.manualName,
            amountMinorUnits = entered.amountMinorUnits,
            amountLabel = amountLabel(entered),
            reference = entered.reference,
            amountProblem = entered.amountProblem,
            fieldErrors = entered.fieldErrors,
            availableBalanceMinorUnits = availableBalanceMinorUnits(),
        )
    }

    private fun amountLabel(entered: Form): String {
        val minor = entered.amountMinorUnits.toLongOrNull() ?: return ""
        return formatMinorUnits(minor, debtorAccount()?.currency.orEmpty())
    }

    private fun accounts(): List<AccountWithBalance> =
        (accountsScreen.value as? ScreenState.Content)?.data.orEmpty()

    private fun beneficiaries(): List<BeneficiaryItem> =
        (beneficiariesScreen.value as? ScreenState.Content)?.data.orEmpty()

    private fun debtorAccount(): BankAccount? =
        accounts().firstOrNull { it.account.accountId == form.value.debtorAccountId }?.account

    private fun availableBalanceMinorUnits(): Long? =
        accounts()
            .firstOrNull { it.account.accountId == form.value.debtorAccountId }
            ?.balance
            ?.availableAmount
            ?.let(::parseMinorUnits)

    /** Whether the destination is one of the PSU's own accounts, which selects `TransferToSelf`. */
    private fun isOwnAccount(identification: String): Boolean {
        val digits = identification.filter(Char::isDigit)
        if (digits.isEmpty()) return false
        return accounts().any { (it.account.sortCode + it.account.accountNumber) == digits }
    }
}

private fun AccountWithBalance.toPickerRow(): SendMoneyPickerRow = SendMoneyPickerRow(
    id = account.accountId,
    initials = initialsOf(account.nickname),
    headline = account.nickname,
    supporting = balance?.let { formatMinorUnits(parseMinorUnits(it.availableAmount) ?: 0L, it.currency) }
        .orEmpty(),
)

private fun BeneficiaryItem.toPickerRow(): SendMoneyPickerRow = SendMoneyPickerRow(
    id = beneficiaryId,
    initials = initialsOf(creditorName),
    headline = creditorName,
    supporting = schemeLabelFor(scheme, identification),
)

private fun schemeLabel(creditor: CreditorSelection): String =
    schemeLabelFor(creditor.scheme, creditor.identification)

/**
 * The identifier as it is written down rather than as it is transmitted — a sort code reads in
 * pairs, an IBAN in fours, and neither is how OBIE carries it.
 */
private fun schemeLabelFor(scheme: BeneficiaryScheme, identification: String): String = when (scheme) {
    BeneficiaryScheme.SortCode -> {
        val digits = identification.filter(Char::isDigit)
        val sortCode = digits.take(SORT_CODE_DIGITS)
        val accountNumber = digits.drop(SORT_CODE_DIGITS)
        "Sort Code · ${formatSortCode(sortCode)} $accountNumber".trimEnd()
    }

    BeneficiaryScheme.Iban -> "IBAN · $identification"
    BeneficiaryScheme.Paym -> "Paym · $identification"
    BeneficiaryScheme.Card -> "Card · $identification"
    BeneficiaryScheme.Account -> identification
}

/** Up to two letters from the name, for the row avatar. */
private fun initialsOf(name: String): String {
    val words = name.split(' ').filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words.first().take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}
