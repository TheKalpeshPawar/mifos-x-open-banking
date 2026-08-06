/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.banking.payment.CreditorSelection
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.PaymentStatus
import org.mifosx.openbanking.core.model.banking.payment.StagedConsent
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAccountRow
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAmountProblem
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyErrorKind
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyPickerRow
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyStage
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyState
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyStep
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyUiState

/**
 * The sandbox capture the whole spec is told through: £850.00 from the PSU's current account to
 * Jameson Lettings, consent 812774903, payment PMT-812774903-01.
 *
 * Shared by the ViewModel, desktop UI, Robolectric and screenshot suites so all four assert against
 * one payment rather than four unrelated ones.
 */
object SendMoneyFixtures {

    const val CURRENT_ACCOUNT_ID = "123456791"
    const val SAVINGS_ACCOUNT_ID = "1123456841"
    const val CREDIT_CARD_ID = "1123456842"
    const val GLOBAL_MONEY_ID = "1123456843"
    const val JAMESON_ID = "BEN-001"
    const val SHARMA_ID = "BEN-002"
    const val EDF_ID = "BEN-003"
    const val CONSENT_ID = "812774903"
    const val PAYMENT_ID = "PMT-812774903-01"
    const val SUPPORT_REFERENCE = "9b7e4d20-1a6c-4f88-9d3a-2c5b7e10f4a6"

    /** £21,530.92 available — comfortably covers the fixture payment. */
    fun currentAccount(): BankAccount = BankAccount(
        accountId = CURRENT_ACCOUNT_ID,
        nickname = "Current account ·· 3349",
        accountSubType = "CurrentAccount",
        currency = "GBP",
        sortCode = "802001",
        accountNumber = "10203349",
        rawIdentification = "80200110203349",
    )

    /** £482.10 available — the account TC-SEND-003 overdraws. */
    fun savingsAccount(): BankAccount = BankAccount(
        accountId = SAVINGS_ACCOUNT_ID,
        nickname = "BMM ACCOUNT ·· 3695",
        accountSubType = "Savings",
        currency = "GBP",
        sortCode = "801225",
        accountNumber = "90953695",
        rawIdentification = "80122590953695",
    )

    /**
     * A credit card, as HSBC returns one: the account-number field is a masked PAN slice, not a
     * real account number, which is why it cannot fund a payment. Present in [accounts] on purpose
     * — the picker must be proven to exclude it, and a fixture without one proves nothing.
     */
    fun creditCard(): BankAccount = BankAccount(
        accountId = CREDIT_CARD_ID,
        nickname = "",
        // "CARD", not "CreditCard": v4.0 has no AccountSubType, so AccountMapper falls through to
        // AccountTypeCode, which HSBC sends as CARD. Both resolve, but the fixture mirrors the bank.
        accountSubType = "CARD",
        currency = "GBP",
        sortCode = "",
        accountNumber = "xxxx-xxxx-xxxx-3456",
        rawIdentification = "xxxx-xxxx-xxxx-3456",
    )

    /**
     * The Global Money wallet exactly as HSBC returns it: `AccountTypeCode: CACC`, so
     * `accountSubType` reads "CACC" and nothing on the domain model distinguishes it from a current
     * account. It stays in [accounts] on purpose — the app cannot predict that this is unfundable,
     * and the tests should reflect that rather than pretend otherwise. What it CAN do is learn from
     * the bank's refusal, which is what the registry-driven filter covers.
     */
    fun globalMoneyWallet(): BankAccount = BankAccount(
        accountId = GLOBAL_MONEY_ID,
        nickname = "",
        accountSubType = "CACC",
        currency = "GBP",
        sortCode = "801197",
        accountNumber = "70009652",
        rawIdentification = "80119770009652",
    )

    fun accounts(): List<AccountWithBalance> = listOf(
        AccountWithBalance(
            account = currentAccount(),
            balance = AccountBalance(
                accountId = CURRENT_ACCOUNT_ID,
                currency = "GBP",
                currentAmount = "21530.92",
                availableAmount = "21530.92",
            ),
        ),
        AccountWithBalance(
            account = savingsAccount(),
            balance = AccountBalance(
                accountId = SAVINGS_ACCOUNT_ID,
                currency = "GBP",
                currentAmount = "482.10",
                availableAmount = "482.10",
            ),
        ),
        AccountWithBalance(
            account = creditCard(),
            balance = AccountBalance(
                accountId = CREDIT_CARD_ID,
                currency = "GBP",
                currentAmount = "245865.06",
                availableAmount = "245865.06",
            ),
        ),
        AccountWithBalance(
            account = globalMoneyWallet(),
            balance = AccountBalance(
                accountId = GLOBAL_MONEY_ID,
                currency = "GBP",
                currentAmount = "303167.25",
                availableAmount = "303167.25",
            ),
        ),
    )

    fun beneficiaries(): List<BeneficiaryItem> = listOf(
        BeneficiaryItem(
            beneficiaryId = JAMESON_ID,
            accountId = CURRENT_ACCOUNT_ID,
            creditorName = "Jameson Lettings",
            scheme = BeneficiaryScheme.SortCode,
            identification = "40120965872310",
            reference = "RENT-FLAT12",
        ),
        BeneficiaryItem(
            beneficiaryId = SHARMA_ID,
            accountId = CURRENT_ACCOUNT_ID,
            creditorName = "John Sharma",
            scheme = BeneficiaryScheme.SortCode,
            identification = "23058011223344",
            reference = "FAMILY",
        ),
        BeneficiaryItem(
            beneficiaryId = EDF_ID,
            accountId = CURRENT_ACCOUNT_ID,
            creditorName = "EDF Energy",
            scheme = BeneficiaryScheme.SortCode,
            identification = "60000199887766",
            reference = "ELEC-8841",
        ),
    )

    fun stagedConsent(): StagedConsent = StagedConsent(
        consentId = CONSENT_ID,
        status = "AWAU",
        authorizationUrl = "https://sandbox.ob.hsbc.co.uk/authorize?request=jwt",
        state = "state-1",
        nonce = "nonce-1",
    )

    fun receipt(): PaymentReceipt = PaymentReceipt(
        domesticPaymentId = PAYMENT_ID,
        consentId = CONSENT_ID,
        status = PaymentStatus.AcceptedSettlementInProcess,
        creationDateTime = "2026-08-05T10:44:05+00:00",
        statusUpdateDateTime = "2026-08-05T10:44:05+00:00",
        amountLabel = "£850.00",
        creditorName = "Jameson Lettings",
    )

    /**
     * Raw fields, not a finished label — the readable name is resolved at render by
     * `accountDisplayName`, exactly as it is in production. A fixture that pre-baked the label would
     * have passed while the live screen rendered blank rows, which is what happened.
     */
    private fun debtorRows(): List<SendMoneyAccountRow> = listOf(
        SendMoneyAccountRow(
            id = CURRENT_ACCOUNT_ID,
            nickname = "",
            accountSubType = "CurrentAccount",
            accountNumber = "10203349",
            rawIdentification = "80200110203349",
            supporting = "£21,530.92",
        ),
        SendMoneyAccountRow(
            id = SAVINGS_ACCOUNT_ID,
            nickname = "",
            accountSubType = "Savings",
            accountNumber = "90953695",
            rawIdentification = "80122590953695",
            supporting = "£482.10",
        ),
    )

    private fun creditorRows(): List<SendMoneyPickerRow> = listOf(
        SendMoneyPickerRow(JAMESON_ID, "JL", "Jameson Lettings", "Sort Code · 40-12-09 65872310"),
        SendMoneyPickerRow(SHARMA_ID, "JS", "John Sharma", "Sort Code · 23-05-80 11223344"),
        SendMoneyPickerRow(EDF_ID, "EE", "EDF Energy", "Sort Code · 60-00-01 99887766"),
    )

    fun loadingState(): SendMoneyState = SendMoneyState(uiState = SendMoneyUiState.Loading)

    fun recipientState(
        beneficiaries: List<SendMoneyPickerRow> = creditorRows(),
        manualEntryVisible: Boolean = false,
    ): SendMoneyState = SendMoneyState(
        uiState = SendMoneyUiState.Content(
            step = SendMoneyStep.Recipient,
            debtorAccounts = listOf(currentAccount(), savingsAccount()),
            debtorRows = debtorRows(),
            beneficiaries = beneficiaries,
            debtorAccountId = CURRENT_ACCOUNT_ID,
            manualEntryVisible = manualEntryVisible,
            availableBalanceMinorUnits = 2_153_092L,
        ),
    )

    /** The chosen payee, as the amount and review steps carry it forward. */
    fun jamesonSelection(): CreditorSelection = CreditorSelection(
        name = "Jameson Lettings",
        scheme = BeneficiaryScheme.SortCode,
        identification = "40120965872310",
        beneficiaryId = JAMESON_ID,
    )

    fun amountState(
        amountMinorUnits: String = "85000",
        problem: SendMoneyAmountProblem? = null,
    ): SendMoneyState = SendMoneyState(
        uiState = SendMoneyUiState.Content(
            step = SendMoneyStep.Amount,
            debtorAccounts = listOf(currentAccount(), savingsAccount()),
            debtorRows = debtorRows(),
            beneficiaries = creditorRows(),
            debtorAccountId = CURRENT_ACCOUNT_ID,
            creditor = jamesonSelection(),
            creditorLabel = "Jameson Lettings",
            amountMinorUnits = amountMinorUnits,
            amountLabel = "£850.00",
            reference = "RENT-FLAT12",
            amountProblem = problem,
            availableBalanceMinorUnits = 2_153_092L,
        ),
    )

    fun reviewState(reference: String = "RENT-FLAT12"): SendMoneyState = SendMoneyState(
        uiState = SendMoneyUiState.Content(
            step = SendMoneyStep.Review,
            debtorAccounts = listOf(currentAccount(), savingsAccount()),
            debtorRows = debtorRows(),
            beneficiaries = creditorRows(),
            debtorAccountId = CURRENT_ACCOUNT_ID,
            debtorAccountRow = debtorRows().first(),
            creditor = jamesonSelection(),
            creditorLabel = "Jameson Lettings",
            creditorSupporting = "Sort Code · 40-12-09 65872310",
            amountMinorUnits = "85000",
            amountLabel = "£850.00",
            reference = reference,
            availableBalanceMinorUnits = 2_153_092L,
        ),
    )

    fun submittingState(
        stage: SendMoneyStage = SendMoneyStage.AwaitingAuthorisation,
    ): SendMoneyState = SendMoneyState(
        uiState = SendMoneyUiState.Submitting(
            stage = stage,
            amountLabel = "£850.00",
            creditorName = "Jameson Lettings",
            consentId = CONSENT_ID,
        ),
    )

    fun errorState(
        kind: SendMoneyErrorKind = SendMoneyErrorKind.OutsideControlParameters,
        supportReference: String? = SUPPORT_REFERENCE,
    ): SendMoneyState = SendMoneyState(
        uiState = SendMoneyUiState.Error(kind = kind, supportReference = supportReference),
    )
}
