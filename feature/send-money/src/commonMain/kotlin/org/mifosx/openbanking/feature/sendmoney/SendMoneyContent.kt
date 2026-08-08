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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.sendmoney.components.RailToggle
import org.mifosx.openbanking.feature.sendmoney.components.SendMoneyAmountCard
import org.mifosx.openbanking.feature.sendmoney.components.SendMoneyDropdownChip
import org.mifosx.openbanking.feature.sendmoney.components.SendMoneyDropdownField
import org.mifosx.openbanking.feature.sendmoney.components.SendMoneyPayeeAvatarRow
import org.mifosx.openbanking.feature.sendmoney.components.SendMoneyPayerPicker
import org.mifosx.openbanking.feature.sendmoney.components.chargeBearerLabel
import org.mifosx.openbanking.feature.sendmoney.components.currencyName
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_exceeds_balance
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_not_a_number
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_not_positive
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_too_many_decimals
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_charges_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_creditor_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_currency_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_currency_mismatch
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_currency_mismatch_bank_choice
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_debtor_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_form_trust_note
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_account_number
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_account_number_error
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_confirm
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_iban
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_iban_error
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_name
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_sort_code
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_sort_code_error
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_no_saved_payees_body
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_no_saved_payees_title
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_non_gbp_notice
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_payee_needs_payer
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_reference_helper
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_reference_label
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_button
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_selected_a11y
import org.mifosx.openbanking.feature.sendmoney.ui.OFFERED_CHARGE_BEARERS
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
    Column(
        modifier = modifier.fillMaxSize().testTag(SendMoneyTestTags.FORM_PAGE),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = FormMaxWidth)
                    .fillMaxWidth()
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
        }
        FormActions(state, onAction)
    }
}

@Composable
private fun PayerSection(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HeadingGap)) {
        SectionHeading(stringResource(Res.string.feature_send_money_debtor_heading))
        SendMoneyPayerPicker(
            rows = state.debtorRows,
            selectedId = state.debtorAccountId,
            letBankChoose = state.letBankChoosePayer,
            expanded = state.payerPickerExpanded,
            onToggle = { onAction(SendMoneyAction.TogglePayerPicker) },
            onSelect = { onAction(SendMoneyAction.SelectDebtorAccount(it)) },
            onLetBankChoose = { onAction(SendMoneyAction.LetBankChoosePayer) },
        )
        if (state.showsConversionAdvisory) {
            ConversionNotice(instructedCurrency = state.instructedCurrency)
        }
    }
}

/** The account holds one currency and the amount is instructed in another, so say so up front. */
@Composable
private fun ConversionNotice(instructedCurrency: String) {
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
            text = stringResource(Res.string.feature_send_money_non_gbp_notice, instructedCurrency),
            style = KptTheme.typography.bodySmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The payees, and the way in for someone who is not one.
 *
 * The avatar row is rendered in every case, including when there are none to show, because its first
 * item is "Add new" — the only route to paying an unsaved account. Dropping the row when the list is
 * empty would take that away exactly when it is needed. The two empty cases still say which they
 * are: no payer chosen yet, or a payer with nothing saved against it.
 */
@Composable
private fun PayeeSection(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HeadingGap)) {
        SectionHeading(stringResource(Res.string.feature_send_money_creditor_heading))
        SendMoneyPayeeAvatarRow(
            // Empty whenever there is no payer, whatever the list happens to hold. Beneficiaries are
            // an account-scoped resource: with no account they are not "not loaded yet", they are
            // the previous account's, and showing them under a payer that no longer exists is the
            // defect this guard closes.
            payees = if (state.payeesUnavailable) emptyList() else state.beneficiaries,
            selectedId = state.creditor?.beneficiaryId,
            selectedLabel = stringResource(Res.string.feature_send_money_selected_a11y),
            onSelect = { onAction(SendMoneyAction.SelectCreditor(it)) },
            onAddNew = { onAction(SendMoneyAction.ShowManualCreditorEntry) },
            modifier = Modifier.testTag(SendMoneyTestTags.CREDITOR_LIST),
        )
        when {
            // Beneficiaries are saved per account, so without one there is no list to read.
            // Saying so beats showing someone else's payees or an empty row that looks broken.
            state.payeesUnavailable -> ChoosePayerFirstNotice()
            !state.hasBeneficiaries -> NoSavedPayees()
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
    // The bar itself spans the window so the surface behind it is unbroken; only its contents are
    // held to the form's width, which is what keeps the CTA above the fields it acts on.
    Column(
        modifier = Modifier
            .testTag(SendMoneyTestTags.FORM_ACTIONS)
            .fillMaxWidth()
            .background(KptTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = FormMaxWidth)
                .fillMaxWidth()
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
        SendMoneyAmountCard(
            amount = state.amountInput,
            instructedCurrency = state.instructedCurrency,
            balanceLabel = state.availableBalanceLabel,
            errorMessage = state.amountProblem?.let { stringResource(it.messageResource()) },
            onAmountChange = { onAction(SendMoneyAction.EnterAmount(it)) },
        ) {
            // Domestic instructs in sterling and offers no choice, so the card is unchanged there.
            if (state.rail == PaymentRail.International) {
                InstructedCurrencyControl(state, onAction)
            }
        }

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
            InternationalFields(state, onAction)
        }
    }
}

/**
 * The instructed currency, inline with the figure it denominates.
 *
 * The code alone on the control and the currency spelled out in the menu: beside a 36sp amount the
 * unit is read as part of the number, so "US Dollar (USD)" there would compete with the figure — but
 * a menu of nineteen bare codes would ask the customer to already know them.
 */
@Composable
private fun InstructedCurrencyControl(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    SendMoneyDropdownChip(
        label = state.instructedCurrency,
        selected = state.instructedCurrency,
        options = state.offeredCurrencies,
        optionLabel = { currencyName(it) },
        optionTestTag = SendMoneyTestTags::instructedCurrencyOption,
        onSelect = { onAction(SendMoneyAction.SelectInstructedCurrency(it)) },
        // Lifted off the baseline by the same amount as the currency symbol on the other side of the
        // figure, so the two sit level rather than the chip hanging below the digits.
        modifier = Modifier
            .testTag(SendMoneyTestTags.INSTRUCTED_CURRENCY_PICKER)
            .padding(bottom = HeadingGap),
    )
}

/**
 * The two fields only an international payment carries, as two dropdowns.
 *
 * They were two rows of `FilterChip`s. Nineteen currencies cannot be chips at all, and the four
 * charge chips wrapped onto a second row that left one option stranded by itself.
 *
 * There is deliberately no "recipient receives" figure anywhere near this. Charges are deducted
 * downstream and `ExchangeRateInformation` is refused `U005`, so no rate can be quoted before
 * authorisation — the currency is named, and no amount is claimed in it.
 */
@Composable
private fun InternationalFields(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    // One Column rather than two siblings: a composable emitting from more than one root leaves its
    // caller deciding the gap between the halves, and the two headed blocks below want the section
    // spacing the rest of the form uses.
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        Column(verticalArrangement = Arrangement.spacedBy(HeadingGap)) {
            SectionHeading(stringResource(Res.string.feature_send_money_currency_heading))
            SendMoneyDropdownField(
                label = currencyName(state.currencyOfTransfer),
                selected = state.currencyOfTransfer,
                options = state.offeredCurrencies,
                optionLabel = { currencyName(it) },
                optionTestTag = SendMoneyTestTags::transferCurrencyOption,
                onSelect = { onAction(SendMoneyAction.SelectCurrencyOfTransfer(it)) },
                modifier = Modifier.testTag(SendMoneyTestTags.CURRENCY_PICKER),
            )
            if (!state.instructedCurrencyValid) {
                CurrencyMismatchNotice(state)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(HeadingGap)) {
            SectionHeading(stringResource(Res.string.feature_send_money_charges_heading))
            SendMoneyDropdownField(
                label = chargeBearerLabel(state.chargeBearer),
                selected = state.chargeBearer,
                options = OFFERED_CHARGE_BEARERS,
                optionLabel = { chargeBearerLabel(it) },
                optionTestTag = SendMoneyTestTags::chargeBearerOption,
                onSelect = { onAction(SendMoneyAction.SelectChargeBearer(it)) },
                modifier = Modifier.testTag(SendMoneyTestTags.CHARGE_BEARER_PICKER),
            )
        }
    }
}

/**
 * The combination the bank refuses, named in the codes that would work.
 *
 * Sits under the transfer currency rather than under the amount because both of those are things the
 * customer can change to resolve it, and the message says so. It is not a correction: a currency
 * somebody chose must not be swapped out from under them, so this blocks the review instead.
 */
@Composable
private fun CurrencyMismatchNotice(state: SendMoneyUiState.Content) {
    Text(
        text = if (state.debtorCurrency.isBlank()) {
            stringResource(
                Res.string.feature_send_money_currency_mismatch_bank_choice,
                state.currencyOfTransfer,
            )
        } else {
            stringResource(
                Res.string.feature_send_money_currency_mismatch,
                state.debtorCurrency,
                state.currencyOfTransfer,
            )
        },
        style = KptTheme.typography.bodySmall,
        color = KptTheme.colorScheme.error,
        modifier = Modifier.testTag(SendMoneyTestTags.CURRENCY_MISMATCH),
    )
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
    SendMoneyAmountProblem.TooManyDecimals -> Res.string.feature_send_money_amount_error_too_many_decimals
    SendMoneyAmountProblem.NotPositive -> Res.string.feature_send_money_amount_error_not_positive
    SendMoneyAmountProblem.ExceedsAvailableBalance -> Res.string.feature_send_money_amount_error_exceeds_balance
}
