/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.vrpsetup

import kotlinx.datetime.LocalDate
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.vrp.PeriodType
import org.mifosx.openbanking.feature.vrpsetup.setup.SetupFormUi
import org.mifosx.openbanking.feature.vrpsetup.setup.SetupPhase
import org.mifosx.openbanking.feature.vrpsetup.setup.StagingUi
import org.mifosx.openbanking.feature.vrpsetup.setup.VrpSetupErrorKind
import org.mifosx.openbanking.feature.vrpsetup.setup.VrpSetupState
import org.mifosx.openbanking.feature.vrpsetup.setup.VrpSetupUiState

/** Fixtures shared by the setup unit, UI and screenshot suites. */
object VrpSetupFixtures {

    const val CURRENT_ACCOUNT_ID = "123456791"
    const val SAVINGS_ACCOUNT_ID = "1123456841"

    /** The end date the mockup shows, and one that clears the two-day floor. */
    val END_DATE: LocalDate = LocalDate(2027, 3, 18)

    val EARLIEST_END_DATE: LocalDate = LocalDate(2026, 8, 21)

    fun payers(): List<AccountWithBalance> = listOf(
        AccountWithBalance(
            account = BankAccount(
                accountId = CURRENT_ACCOUNT_ID,
                accountHolderName = "Everyday Current Account",
                accountTypeCode = "CACC",
                currency = "GBP",
                identification = "80200110204021",
                scheme = AccountScheme.SortCode,
            ),
            balance = AccountBalance(CURRENT_ACCOUNT_ID, "GBP", "3482.19", "3482.19"),
        ),
        AccountWithBalance(
            account = BankAccount(
                accountId = SAVINGS_ACCOUNT_ID,
                accountHolderName = "BMM ACCOUNT",
                accountTypeCode = "SVGS",
                currency = "GBP",
                identification = "80122590953695",
                scheme = AccountScheme.SortCode,
            ),
            balance = AccountBalance(SAVINGS_ACCOUNT_ID, "GBP", "482.10", "482.10"),
        ),
    )

    fun payees(): List<BeneficiaryItem> = listOf(
        payee("40478412345678", "Sarah Chen"),
        payee("40478487654321", "Oakwood Property Ltd"),
        payee("40478411223344", "James Whitfield"),
    )

    private fun payee(identification: String, name: String) = BeneficiaryItem(
        beneficiaryId = identification,
        accountId = CURRENT_ACCOUNT_ID,
        creditorName = name,
        scheme = BeneficiaryScheme.SortCode,
        identification = identification,
        reference = "",
    )

    /** The form the mockup shows: a chosen payer, a saved payee, both ceilings and no end date. */
    fun filledForm(
        payNewSelected: Boolean = false,
        payerExpanded: Boolean = false,
        chooseAtBank: Boolean = false,
    ) = SetupFormUi(
        payerOptions = payers(),
        selectedPayerId = if (chooseAtBank) null else CURRENT_ACCOUNT_ID,
        chooseAtBank = chooseAtBank,
        payerExpanded = payerExpanded,
        payeeOptions = payees(),
        selectedPayeeId = if (payNewSelected) null else "40478412345678",
        payNewSelected = payNewSelected,
        perPaymentAmount = "200.00",
        periodType = PeriodType.Month,
        periodicAmount = "500.00",
        earliestSelectableDate = EARLIEST_END_DATE,
    )

    fun emptyForm() = SetupFormUi(
        payerOptions = payers(),
        earliestSelectableDate = EARLIEST_END_DATE,
    )

    fun formState(
        form: SetupFormUi = filledForm(),
        phase: SetupPhase = SetupPhase.Form,
        staging: StagingUi = StagingUi.NotStarted,
    ) = VrpSetupState(VrpSetupUiState.Content(phase = phase, form = form, staging = staging))

    fun reviewState(staging: StagingUi = StagingUi.NotStarted) =
        formState(phase = SetupPhase.Review, staging = staging)

    fun loadingState() = VrpSetupState(VrpSetupUiState.Loading)

    fun noAccountsState() = VrpSetupState(VrpSetupUiState.NoEligiblePayers)

    fun errorState(
        kind: VrpSetupErrorKind = VrpSetupErrorKind.AccountsUnavailable,
    ) = VrpSetupState(VrpSetupUiState.Error(kind))
}
