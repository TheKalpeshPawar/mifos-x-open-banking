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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.sendmoney.components.ChargeBearerPicker
import org.mifosx.openbanking.feature.sendmoney.components.RailToggle
import org.mifosx.openbanking.feature.sendmoney.components.RecipientCurrencyPicker
import org.mifosx.openbanking.feature.sendmoney.components.SendMoneyAccountPickerList
import org.mifosx.openbanking.feature.sendmoney.components.SendMoneyPickerList
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_exceeds_balance
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_not_a_number
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_not_positive
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_label
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_charges_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_creditor_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_currency_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_debtor_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_form_trust_note
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_account_number
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_account_number_error
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_confirm
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_entry
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_entry_a11y
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_iban
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_iban_error
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_name
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_sort_code
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_sort_code_error
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_no_saved_payees_body
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_no_saved_payees_title
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_non_gbp_notice
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_payee_needs_payer
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_payer_bank_choice
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_payer_bank_choice_supporting
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_reference_helper
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_reference_label
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_button
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_selected_a11y
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAction
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAmountProblem
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyStep
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyUiState
import template.core.base.designsystem.theme.KptTheme

private const val REFERENCE_MAX_LENGTH = 35

/**
 * The form and its review, as two pages of one state.
 *
 * A step field rather than two screen states: what the customer chose stays live across the pages,
 * and coming back from a failure must not discard it.
 */
@Composable
internal fun SendMoneyContent(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.step) {
        SendMoneyStep.Form -> SendMoneyFormPage(state, onAction, modifier)
        SendMoneyStep.Review -> SendMoneyReviewPage(state, onAction, modifier)
    }
}

/**
 * Payer, payee and amount on one scroll, with the action pinned below it.
 *
 * The three used to be separate steps. Nothing about them needed sequencing — none of the three
 * constrains what the others may be — so walking the customer through them only hid how short the
 * form actually is. Pinning the action matters once they are combined: the page is now long enough
 * that a button at the end of the scroll would sit off screen on a phone.
 */
@Composable
private fun SendMoneyFormPage(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().testTag(SendMoneyTestTags.FORM_PAGE)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SectionGap),
        ) {
            RailToggle(
                rail = state.rail,
                onSelect = { onAction(SendMoneyAction.SelectRail(it)) },
            )
            PayerSection(state, onAction)
            PayeeSection(state, onAction)
            AmountSection(state, onAction)
        }
        FormActions(state, onAction)
    }
}

@Composable
private fun PayerSection(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        SectionHeading(stringResource(Res.string.feature_send_money_debtor_heading))
        SendMoneyAccountPickerList(
            rows = state.debtorRows,
            selectedId = state.debtorAccountId,
            onSelect = { onAction(SendMoneyAction.SelectDebtorAccount(it)) },
            tagFor = SendMoneyTestTags::debtorRow,
            selectedLabel = stringResource(Res.string.feature_send_money_selected_a11y),
            modifier = Modifier.testTag(SendMoneyTestTags.DEBTOR_LIST),
        )
        BankChoosesPayerRow(
            selected = state.letBankChoosePayer,
            onClick = { onAction(SendMoneyAction.LetBankChoosePayer) },
        )
        if (state.showsNonGbpAdvisory) {
            NonGbpPayerNotice()
        }
    }
}

/**
 * The "don't send a payer" option, as a peer of the accounts rather than an absence.
 *
 * Sending no `DebtorAccount` is a shape the bank supports, not a form left incomplete, so it is
 * offered as something to choose. It sits outside the picker list because it is not an account —
 * putting it in the list would make it selectable by the same code that keys payees by account id.
 */
@Composable
private fun BankChoosesPayerRow(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SendMoneyTestTags.PAYER_BANK_CHOICE)
            .clip(RoundedCornerShape(NoticeCorner))
            .background(
                if (selected) KptTheme.colorScheme.primaryContainer else KptTheme.colorScheme.surface,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(NoticePadding),
        horizontalArrangement = Arrangement.spacedBy(NoticeGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = null,
            tint = KptTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(HeroGap)) {
            Text(
                text = stringResource(Res.string.feature_send_money_payer_bank_choice),
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.feature_send_money_payer_bank_choice_supporting),
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The amount is instructed in sterling whatever the payer holds, so say so before it is entered. */
@Composable
private fun NonGbpPayerNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SendMoneyTestTags.NON_GBP_NOTICE)
            .clip(RoundedCornerShape(NoticeCorner))
            .background(KptTheme.colorScheme.surfaceContainer)
            .padding(NoticePadding),
        horizontalArrangement = Arrangement.spacedBy(NoticeGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = KptTheme.colorScheme.outline,
            modifier = Modifier.size(NoticeIconSize),
        )
        Text(
            text = stringResource(Res.string.feature_send_money_non_gbp_notice),
            style = KptTheme.typography.bodySmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PayeeSection(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        SectionHeading(stringResource(Res.string.feature_send_money_creditor_heading))
        when {
            // Beneficiaries are saved per account, so without one there is no list to read.
            // Saying so beats showing someone else's payees or an empty list that looks broken.
            state.payeesUnavailable -> ChoosePayerFirstNotice()

            state.hasBeneficiaries -> SendMoneyPickerList(
                rows = state.beneficiaries,
                selectedId = state.creditor?.beneficiaryId,
                onSelect = { onAction(SendMoneyAction.SelectCreditor(it)) },
                tagFor = SendMoneyTestTags::creditorRow,
                selectedLabel = stringResource(Res.string.feature_send_money_selected_a11y),
                modifier = Modifier.testTag(SendMoneyTestTags.CREDITOR_LIST),
            )

            else -> NoSavedPayees()
        }

        TextButton(
            onClick = { onAction(SendMoneyAction.ShowManualCreditorEntry) },
            modifier = Modifier.testTag(SendMoneyTestTags.MANUAL_ENTRY_BUTTON),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(Res.string.feature_send_money_manual_entry_a11y),
            )
            Text(
                text = stringResource(Res.string.feature_send_money_manual_entry),
                modifier = Modifier.padding(start = HeadingGap),
            )
        }

        if (state.manualEntryVisible) {
            ManualCreditorFields(state, onAction)
        }
    }
}

/** The action bar, pinned so it stays reachable however far the form has been scrolled. */
@Composable
private fun FormActions(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SendMoneyTestTags.FORM_ACTIONS)
            .background(KptTheme.colorScheme.surface)
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(HeroGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_send_money_review_button),
            onClick = { onAction(SendMoneyAction.ReviewPayment) },
            testTag = SendMoneyTestTags.REVIEW_BUTTON,
            enabled = state.canReview,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(NoticeGap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag(SendMoneyTestTags.FORM_TRUST_NOTE),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = KptTheme.colorScheme.outline,
                modifier = Modifier.size(NoticeIconSize),
            )
            Text(
                text = stringResource(Res.string.feature_send_money_form_trust_note),
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ManualCreditorFields(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        OutlinedTextField(
            value = state.manualName,
            onValueChange = { onAction(SendMoneyAction.EnterManualName(it)) },
            label = { Text(stringResource(Res.string.feature_send_money_manual_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.MANUAL_NAME),
        )
        // The rails identify a creditor differently — sort code and account number against an IBAN —
        // and each refuses the other's scheme with U027, so only one set is ever offered.
        if (state.rail == PaymentRail.Domestic) {
            OutlinedTextField(
                value = state.manualSortCode,
                onValueChange = { onAction(SendMoneyAction.EnterManualSortCode(it)) },
                label = { Text(stringResource(Res.string.feature_send_money_manual_sort_code)) },
                isError = state.fieldErrors.sortCodeInvalid,
                supportingText = {
                    if (state.fieldErrors.sortCodeInvalid) {
                        Text(stringResource(Res.string.feature_send_money_manual_sort_code_error))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.MANUAL_SORT_CODE),
            )
            OutlinedTextField(
                value = state.manualAccountNumber,
                onValueChange = { onAction(SendMoneyAction.EnterManualAccountNumber(it)) },
                label = { Text(stringResource(Res.string.feature_send_money_manual_account_number)) },
                isError = state.fieldErrors.accountNumberInvalid,
                supportingText = {
                    if (state.fieldErrors.accountNumberInvalid) {
                        Text(stringResource(Res.string.feature_send_money_manual_account_number_error))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.MANUAL_ACCOUNT_NUMBER),
            )
        } else {
            OutlinedTextField(
                value = state.manualIban,
                onValueChange = { onAction(SendMoneyAction.EnterManualIban(it)) },
                label = { Text(stringResource(Res.string.feature_send_money_manual_iban)) },
                isError = state.fieldErrors.ibanInvalid,
                supportingText = {
                    if (state.fieldErrors.ibanInvalid) {
                        Text(
                            text = stringResource(Res.string.feature_send_money_manual_iban_error),
                            modifier = Modifier.testTag(SendMoneyTestTags.MANUAL_IBAN_ERROR),
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.MANUAL_IBAN),
            )
        }
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_send_money_manual_confirm),
            onClick = { onAction(SendMoneyAction.ConfirmManualCreditor) },
            testTag = SendMoneyTestTags.MANUAL_CONFIRM,
        )
    }
}

@Composable
private fun AmountSection(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        SectionHeading(stringResource(Res.string.feature_send_money_amount_heading))

        OutlinedTextField(
            value = state.amountMinorUnits,
            onValueChange = { onAction(SendMoneyAction.EnterAmount(it)) },
            label = { Text(stringResource(Res.string.feature_send_money_amount_label)) },
            isError = state.amountProblem != null,
            supportingText = {
                state.amountProblem?.let { problem ->
                    Text(
                        text = stringResource(problem.messageResource()),
                        modifier = Modifier.testTag(SendMoneyTestTags.AMOUNT_ERROR),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.AMOUNT_FIELD),
        )

        // Domestic only. International refuses RemittanceInformation outright with U005, so showing
        // the field there would invite someone to type a reference the recipient never sees.
        if (state.rail == PaymentRail.Domestic) {
            OutlinedTextField(
                value = state.reference,
                onValueChange = { onAction(SendMoneyAction.EnterReference(it.take(REFERENCE_MAX_LENGTH))) },
                label = { Text(stringResource(Res.string.feature_send_money_reference_label)) },
                supportingText = { Text(stringResource(Res.string.feature_send_money_reference_helper)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.REFERENCE_FIELD),
            )
        }

        // International only, and both required there: ChargeBearer is refused on domestic and
        // missing on international earns U004.
        if (state.rail == PaymentRail.International) {
            SectionHeading(stringResource(Res.string.feature_send_money_currency_heading))
            RecipientCurrencyPicker(
                selected = state.currencyOfTransfer,
                currencies = state.transferCurrencies,
                onSelect = { onAction(SendMoneyAction.SelectCurrencyOfTransfer(it)) },
            )

            SectionHeading(stringResource(Res.string.feature_send_money_charges_heading))
            ChargeBearerPicker(
                selected = state.chargeBearer,
                onSelect = { onAction(SendMoneyAction.SelectChargeBearer(it)) },
            )
        }
    }
}

/**
 * Why the payee list is empty when no payer has been chosen.
 *
 * Not an error and not an empty state: the list is account-scoped, so it genuinely cannot be
 * fetched yet. [NoSavedPayees] is the different case where an account was chosen and has none.
 */
@Composable
private fun ChoosePayerFirstNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SendMoneyTestTags.PAYEE_NEEDS_PAYER)
            .clip(RoundedCornerShape(NoticeCorner))
            .background(KptTheme.colorScheme.surfaceContainer)
            .padding(NoticePadding),
        verticalArrangement = Arrangement.spacedBy(HeroGap),
    ) {
        Text(
            text = stringResource(Res.string.feature_send_money_payee_needs_payer),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoSavedPayees() {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.NO_SAVED_PAYEES),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeadingGap),
    ) {
        Text(
            text = stringResource(Res.string.feature_send_money_no_saved_payees_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.feature_send_money_no_saved_payees_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun SendMoneyAmountProblem.messageResource(): StringResource = when (this) {
    SendMoneyAmountProblem.NotANumber -> Res.string.feature_send_money_amount_error_not_a_number
    SendMoneyAmountProblem.NotPositive -> Res.string.feature_send_money_amount_error_not_positive
    SendMoneyAmountProblem.ExceedsAvailableBalance -> Res.string.feature_send_money_amount_error_exceeds_balance
}
