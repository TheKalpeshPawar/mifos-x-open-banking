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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.common.formatMinorUnits
import org.mifosx.openbanking.core.common.formatSortCode
import org.mifosx.openbanking.core.common.parseMinorUnits
import org.mifosx.openbanking.core.data.banking.AccountCapabilityRegistry
import org.mifosx.openbanking.core.data.banking.AccountsOverviewRepository
import org.mifosx.openbanking.core.data.banking.BeneficiariesRepository
import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.data.util.toThrowable
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.banking.payment.ChargeBearer
import org.mifosx.openbanking.core.model.banking.payment.CreditorSelection
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductCapability
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
import org.mifosx.openbanking.feature.sendmoney.components.initialsOf
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.combineContent
import template.core.base.network.NetworkResult
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SORT_CODE_DIGITS = 6
private const val ACCOUNT_NUMBER_DIGITS = 8
private const val INSTRUCTION_ID_PREFIX = "MFX"
private const val END_TO_END_ID_PREFIX = "E2E"

/** ISO 13616 bounds: a country's IBAN is fixed-length, but the shortest is 15 and the longest 34. */
private const val IBAN_MIN_LENGTH = 15
private const val IBAN_MAX_LENGTH = 34
private const val IBAN_COUNTRY_PREFIX_LENGTH = 2

/**
 * What the recipient can be paid in.
 *
 * Global Money accepts only these two, and the sandbox refuses anything else on that path with
 * `U002`. The first is the default when the rail is switched.
 */
private val TRANSFER_CURRENCIES = listOf("USD", "EUR")

/** Written-down IBANs carry spaces and are often lower case; OBIE wants neither. */
private fun String.normaliseIban(): String = filterNot { it.isWhitespace() }.uppercase()

/**
 * Whether a string could be an IBAN at all.
 *
 * Shape only — length, alphanumeric, and the two-letter country prefix followed by check digits. It
 * deliberately stops short of the mod-97 checksum: this is the guard that stops obvious nonsense
 * reaching the bank, not a claim that the account exists.
 */
private fun String.isPlausibleIban(): Boolean = normaliseIban().let { candidate ->
    candidate.length in IBAN_MIN_LENGTH..IBAN_MAX_LENGTH &&
        candidate.all { it.isLetterOrDigit() } &&
        candidate.take(IBAN_COUNTRY_PREFIX_LENGTH).all { it.isLetter() }
}

/** `InstructedAmount.Currency`. GBP on both rails; see [SendMoneyViewModel.buildDraft]. */
private const val INSTRUCTED_CURRENCY = "GBP"

/**
 * Drives the payment form up to the point the customer leaves for their bank.
 *
 * It builds the instruction, stages it, and hands off to the browser. It does **not** submit: the
 * hop to the bank pops this screen without saving state, so the ViewModel that returns is a new and
 * empty one. Confirming funds and submitting therefore belong to the leg that comes back —
 * `PaymentConsentViewModel` — which reads the staged draft out of storage.
 *
 * Two things here are load-bearing and neither is obvious from the shape of the code:
 *
 * The **draft and its two idempotency keys are minted exactly once**, in [confirmAndStageConsent].
 * Regenerating them would produce a second instruction the bank cannot recognise as a duplicate, and
 * the draft is stored rather than rebuilt because the submitted `Initiation` must be byte-identical
 * to the staged one. The two keys differ from each other because the consent and submission bodies
 * differ; see [PaymentDraft].
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
    private val capabilityRegistry: AccountCapabilityRegistry,
) : BaseViewModel<SendMoneyState, SendMoneyEvent, SendMoneyAction>(initialState = SendMoneyState()) {

    /** Everything the PSU has entered. Held apart from loaded data so a refresh cannot clear it. */
    private data class Form(
        val step: SendMoneyStep = SendMoneyStep.Form,
        val rail: PaymentRail = PaymentRail.Domestic,
        val debtorAccountId: String? = null,
        val letBankChoosePayer: Boolean = false,
        val creditor: CreditorSelection? = null,
        val manualEntryVisible: Boolean = false,
        val manualSortCode: String = "",
        val manualAccountNumber: String = "",
        val manualIban: String = "",
        val manualName: String = "",
        val amountMinorUnits: String = "",
        val currencyOfTransfer: String = "GBP",
        val chargeBearer: ChargeBearer = ChargeBearer.BorneByCreditor,
        val reference: String = "",
        val amountProblem: SendMoneyAmountProblem? = null,
        val fieldErrors: SendMoneyFieldErrors = SendMoneyFieldErrors(),
    )

    /** Which of the three screen states is showing. The form only renders under [Phase.Form]. */
    private sealed interface Phase {
        data object Form : Phase
        data class Submitting(val stage: SendMoneyStage, val consentId: String?) : Phase
        data class Error(val kind: SendMoneyErrorKind, val supportReference: String?) : Phase
    }

    /**
     * The loaded accounts, already narrowed to those that may fund a payment.
     *
     * Filtered once here, as the stream lands, so the picker rows, the seeded selection, the
     * balance comparison and the draft's debtor can never disagree about which accounts are
     * payable. See [canFundAPayment] for why a credit card is not one of them.
     */
    private val accountsScreen =
        MutableStateFlow<ScreenState<List<AccountWithBalance>>>(ScreenState.Loading)
    private val beneficiariesScreen =
        MutableStateFlow<ScreenState<List<BeneficiaryItem>>>(ScreenState.Loading)
    private val form = MutableStateFlow(Form())
    private val phase = MutableStateFlow<Phase>(Phase.Form)
    private val selectedAccountId = MutableStateFlow("")

    /**
     * Payees for the chosen payer, re-fetched whenever that changes.
     *
     * A blank id emits an empty list rather than being filtered out. Filtering meant de-selecting an
     * account produced no emission at all, so the previous account's payees stayed in state and on
     * screen under a payer that no longer existed — they are scoped to an account, and with no
     * account the honest answer is none.
     */
    private val beneficiariesStream: Flow<ScreenState<List<BeneficiaryItem>>> =
        // No distinctUntilChanged: a StateFlow already drops equal values, and applying it here is
        // a deprecated no-op. It was meaningful only while a filter() sat in front of this.
        selectedAccountId
            .flatMapLatest { accountId ->
                if (accountId.isBlank()) {
                    flowOf(ScreenState.Content(emptyList(), DataFreshness.FRESH))
                } else {
                    beneficiariesRepository.beneficiariesStream(accountId, viewModelScope).state
                }
            }

    init {
        // Two filters, deliberately different in kind. canFundAPayment is a PREDICTION from the
        // product matrix, and now catches both refused products: the credit card by its subtype and
        // the Global Money wallet by its description. The registry is what the bank has actually
        // refused this session — the safety net for anything the matrix cannot predict. It is only
        // a backstop now, not the sole defence, which matters because it does not survive a restart.
        accountsOverviewRepository.overviewState(viewModelScope)
            .combineContent(capabilityRegistry.unsupportedStream()) { accounts, refused, _ ->
                accounts
                    .filter { it.account.canFundAPayment() }
                    .filterNot {
                        AccountEndpoint.PaymentDebtor in refused[it.account.accountId].orEmpty()
                    }
            }
            .onEach { screen -> accountsScreen.value = screen }
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
            is SendMoneyAction.SelectRail -> selectRail(action.rail)
            is SendMoneyAction.SelectDebtorAccount -> selectDebtor(action.accountId)
            SendMoneyAction.LetBankChoosePayer -> letBankChoosePayer()
            is SendMoneyAction.SelectCreditor -> selectCreditor(action.beneficiaryId)
            SendMoneyAction.ShowManualCreditorEntry -> form.value = form.value.copy(manualEntryVisible = true)
            is SendMoneyAction.EnterManualSortCode -> enterSortCode(action.sortCode)
            is SendMoneyAction.EnterManualAccountNumber -> enterAccountNumber(action.accountNumber)
            is SendMoneyAction.EnterManualIban -> enterIban(action.iban)
            is SendMoneyAction.EnterManualName -> form.value = form.value.copy(manualName = action.name)
            SendMoneyAction.ConfirmManualCreditor -> confirmManualCreditor()
            is SendMoneyAction.EnterAmount -> enterAmount(action.minorUnits)
            is SendMoneyAction.SelectCurrencyOfTransfer ->
                form.value =
                    form.value.copy(currencyOfTransfer = action.currency)
            is SendMoneyAction.SelectChargeBearer ->
                form.value =
                    form.value.copy(chargeBearer = action.bearer)
            is SendMoneyAction.EnterReference -> enterReference(action.reference)
            SendMoneyAction.ReviewPayment -> form.value = form.value.copy(step = SendMoneyStep.Review)
            SendMoneyAction.ConfirmAndStageConsent -> confirmAndStageConsent()
            SendMoneyAction.RetryStaging -> confirmAndStageConsent()
            SendMoneyAction.ChangePayer -> changePayer()
            SendMoneyAction.BackStep -> backStep()
            SendMoneyAction.RetryLoad -> accountsOverviewRepository.refresh()
        }
    }

    /** Changing the payer re-keys the payee list: counterparties are saved per account, not per customer. */
    private fun selectDebtor(accountId: String) {
        selectedAccountId.value = accountId
        form.value = form.value.copy(
            debtorAccountId = accountId,
            letBankChoosePayer = false,
            creditor = null,
        )
    }

    /**
     * Send no `DebtorAccount` and let the PSU pick the account at their bank.
     *
     * A sanctioned shape rather than a gap: HSBC's own international sample omits the block, and
     * both rails accept a consent without it. The payee is dropped with it — beneficiaries are saved
     * per account, so a payee chosen under one payer means nothing once there is no payer at all.
     */
    private fun letBankChoosePayer() {
        selectedAccountId.value = ""
        form.value = form.value.copy(
            debtorAccountId = null,
            letBankChoosePayer = true,
            creditor = null,
        )
    }

    /**
     * Switching rails invalidates the payee and everything typed towards one.
     *
     * The two rails identify a creditor differently — sort code and account number against an IBAN —
     * so a payee chosen under one is not a payee under the other, and half-typed fields for the
     * wrong scheme would be carried into a request that cannot accept them. The **amount survives**
     * on purpose: it means the same thing on both rails, and re-typing it is a cost with no reason.
     *
     * The transfer currency is normalised because it defaults to GBP, which the international rail
     * never offers. Leaving it would stage `CurrencyOfTransfer: GBP` with nothing on screen having
     * said so.
     */
    private fun selectRail(rail: PaymentRail) {
        val current = form.value
        form.value = current.copy(
            rail = rail,
            creditor = null,
            manualEntryVisible = false,
            manualSortCode = "",
            manualAccountNumber = "",
            manualIban = "",
            fieldErrors = SendMoneyFieldErrors(),
            currencyOfTransfer = when (rail) {
                PaymentRail.Domestic -> INSTRUCTED_CURRENCY
                PaymentRail.International ->
                    current.currencyOfTransfer
                        .takeIf { it in TRANSFER_CURRENCIES }
                        ?: TRANSFER_CURRENCIES.first()
            },
        )
    }

    private fun enterIban(raw: String) {
        form.value = form.value.copy(
            manualIban = raw,
            // Quiet until something has been typed: flagging an empty field tells someone they got
            // it wrong before they have had a go.
            fieldErrors = form.value.fieldErrors.copy(
                ibanInvalid = raw.isNotBlank() && !raw.isPlausibleIban(),
            ),
        )
    }

    /**
     * The international rail's manual payee: a name and an IBAN.
     *
     * The IBAN is normalised before it becomes the identification — written down it carries spaces
     * and is often lower case, and OBIE wants it unpunctuated and upper case.
     */
    private fun confirmManualIbanCreditor(current: Form) {
        val iban = current.manualIban.normaliseIban()
        if (!iban.isPlausibleIban()) {
            form.value = current.copy(
                fieldErrors = SendMoneyFieldErrors(ibanInvalid = true),
            )
            return
        }
        form.value = current.copy(
            creditor = CreditorSelection(
                name = current.manualName,
                scheme = BeneficiaryScheme.Iban,
                identification = iban,
                isOwnAccount = isOwnAccount(iban),
            ),
            fieldErrors = SendMoneyFieldErrors(),
        )
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
        if (current.rail == PaymentRail.International) {
            confirmManualIbanCreditor(current)
            return
        }
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
        updateState { copy(draft = draft) }

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

    private fun fail(throwable: Throwable) {
        phase.value = Phase.Error(
            kind = classifySendMoneyError(throwable),
            supportReference = supportReferenceOf(throwable),
        )
    }

    /**
     * Returns to the payer step after the bank refused the account the payment came from.
     *
     * Clears the payer as well as the draft: the account that was selected is the thing that failed,
     * and by now the registry has removed it from the list, so leaving it selected would point at a
     * row that no longer exists. The payee is kept — nothing was wrong with it.
     */
    private fun changePayer() {
        form.value = form.value.copy(
            step = SendMoneyStep.Form,
            debtorAccountId = null,
            letBankChoosePayer = false,
        )
        phase.value = Phase.Form
        updateState { copy(draft = null, consentId = null) }
    }

    /**
     * Returns to the form from a failure, keeping the payer and payee already chosen.
     *
     * The draft is dropped deliberately: editing the amount changes the instruction, so the next
     * attempt is a different payment and must be staged under a new key rather than replayed.
     */
    private fun backStep() {
        form.value = form.value.copy(step = SendMoneyStep.Form)
        phase.value = Phase.Form
        updateState { copy(draft = null, consentId = null) }
    }

    /**
     * Four symmetric guards rather than tangled control flow: the draft can only be built once the
     * payer, payee and amount are all present, and each absence means the same thing — not ready.
     */
    @Suppress("ReturnCount")
    private fun buildDraft(): PaymentDraft? {
        val current = form.value
        val creditor = current.creditor ?: return null
        val amount = current.amountMinorUnits.toLongOrNull() ?: return null
        val hex = Uuid.generateV4().toHexString()
        val isInternational = current.rail == PaymentRail.International
        return PaymentDraft(
            // Null when the PSU left the choice to the bank; both mappers omit the block.
            debtorAccount = debtorAccount(),
            creditor = creditor,
            amountMinorUnits = amount,
            // Always GBP. HSBC instructs in sterling on both rails and converts at their end, so
            // reading this off the payer would send whatever a foreign-currency account happens to
            // hold. What the recipient receives is CurrencyOfTransfer, which is a separate field.
            currency = INSTRUCTED_CURRENCY,
            reference = current.reference.takeIf { it.isNotBlank() && !isInternational },
            instructionIdentification = INSTRUCTION_ID_PREFIX + hex,
            endToEndIdentification = END_TO_END_ID_PREFIX + hex,
            consentIdempotencyKey = Uuid.generateV4().toString(),
            paymentIdempotencyKey = Uuid.generateV4().toString(),
            currencyOfTransfer = if (isInternational) current.currencyOfTransfer else null,
            chargeBearer = if (isInternational) current.chargeBearer else null,
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
        val filteredPayees = payeeList.filter { payee ->
            when (entered.rail) {
                PaymentRail.Domestic -> payee.scheme == BeneficiaryScheme.SortCode
                PaymentRail.International -> payee.scheme == BeneficiaryScheme.Iban
            }
        }
        val selected = accounts.firstOrNull { it.account.accountId == entered.debtorAccountId }
        return SendMoneyUiState.Content(
            step = entered.step,
            rail = entered.rail,
            debtorAccounts = accounts.map { it.account },
            debtorRows = accounts.map { it.toAccountRow() },
            beneficiaries = filteredPayees.map { it.toPickerRow() },
            debtorAccountId = entered.debtorAccountId,
            letBankChoosePayer = entered.letBankChoosePayer,
            creditor = entered.creditor,
            creditorLabel = entered.creditor?.name.orEmpty(),
            creditorSupporting = entered.creditor?.let { schemeLabel(it) }.orEmpty(),
            debtorAccountRow = selected?.toAccountRow(),
            debtorCurrency = selected?.account?.currency.orEmpty(),
            manualEntryVisible = entered.manualEntryVisible,
            manualSortCode = entered.manualSortCode,
            manualAccountNumber = entered.manualAccountNumber,
            manualIban = entered.manualIban,
            manualName = entered.manualName,
            amountMinorUnits = entered.amountMinorUnits,
            amountLabel = amountLabel(entered),
            currencyOfTransfer = entered.currencyOfTransfer,
            transferCurrencies = if (entered.rail == PaymentRail.International) {
                TRANSFER_CURRENCIES
            } else {
                emptyList()
            },
            chargeBearer = entered.chargeBearer,
            reference = entered.reference,
            amountProblem = entered.amountProblem,
            fieldErrors = entered.fieldErrors,
            availableBalanceMinorUnits = availableBalanceMinorUnits(),
        )
    }

    /** Always in the instructed currency — the amount is not the payer account's own currency. */
    private fun amountLabel(entered: Form): String {
        val minor = entered.amountMinorUnits.toLongOrNull() ?: return ""
        return formatMinorUnits(minor, INSTRUCTED_CURRENCY)
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

/**
 * Carries the account's raw fields rather than a finished name.
 *
 * `nickname` is blank on most HSBC accounts, so using it as the headline renders an empty row —
 * which is exactly what shipped before this. The readable label is resolved at render by
 * `core/ui`'s `accountDisplayName`, the same resolver Home and Accounts use.
 */
private fun AccountWithBalance.toAccountRow(): SendMoneyAccountRow = SendMoneyAccountRow(
    id = account.accountId,
    nickname = account.nickname,
    accountSubType = account.accountSubType,
    accountNumber = account.accountNumber,
    rawIdentification = account.rawIdentification,
    supporting = balance?.let { formatMinorUnits(parseMinorUnits(it.availableAmount) ?: 0L, it.currency) }
        .orEmpty(),
)

/**
 * Whether this account's PRODUCT is known to be unable to fund a payment.
 *
 * Catches both products the sandbox refuses as a named payer: the credit card, visible in
 * [BankAccount.accountSubType], and the Global Money wallet, which reports `AccountTypeCode: CACC`
 * and is identifiable **only** by its free-text [BankAccount.description]. That description used to
 * be passed as an empty string here, so every wallet resolved to a plain current account, was
 * offered as a payer, and was removed only after the bank refused it with `U002` — and because the
 * capability registry is in memory, it came back on the next launch to fail the same way.
 *
 * The registry still runs alongside this, and still matters: it catches whatever the product matrix
 * cannot predict. This is the prediction; that is the correction.
 *
 * Fails open through [HsbcProductCapability.supports]: a product this app has never met keeps the
 * payer role and is corrected by the bank, which is the safer default for an unknown.
 */
private fun BankAccount.canFundAPayment(): Boolean = HsbcProductCapability.supports(
    endpoint = AccountEndpoint.PaymentDebtor,
    productType = HsbcProductType.resolve(
        accountSubType = accountSubType,
        accountTypeCode = "",
        description = description,
    ),
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
