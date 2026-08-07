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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.account.accountDisplayName
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.core.ui.components.MifosTonalPillButton
import org.mifosx.openbanking.feature.sendmoney.components.RailToggle
import org.mifosx.openbanking.feature.sendmoney.components.SendMoneyAccountPickerList
import org.mifosx.openbanking.feature.sendmoney.components.SendMoneyPickerList
import org.mifosx.openbanking.feature.sendmoney.components.initialsOf
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_exceeds_balance
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_not_a_number
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_error_not_positive
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_label
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_confirm
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_creditor_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_debtor_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_edit_payment
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_form_trust_note
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_account_number
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_account_number_error
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_confirm
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_entry
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_entry_a11y
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_name
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_sort_code
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_sort_code_error
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_no_saved_payees_body
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_no_saved_payees_title
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_reference_helper
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_reference_label
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_auth_notice
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_button
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_details_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_from
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_hero_caption
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_reference
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_reference_empty
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_sent_via
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_sent_via_value
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_to
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_total
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_selected_a11y
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAction
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAmountProblem
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyStep
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyUiState
import template.core.base.designsystem.theme.KptTheme

private const val REFERENCE_MAX_LENGTH = 35

private val ScreenPadding = 16.dp
private val SectionGap = 16.dp
private val HeadingGap = 8.dp
private val RowGap = 12.dp
private val HeroGap = 4.dp
private val ChipCorner = 20.dp
private val ChipPaddingHorizontal = 10.dp
private val ChipPaddingVertical = 6.dp
private val ChipGap = 8.dp
private val ChipAvatarSize = 24.dp
private val NoticeCorner = 12.dp
private val NoticePadding = 12.dp
private val NoticeGap = 10.dp
private val NoticeIconSize = 16.dp

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
    }
}

@Composable
private fun PayeeSection(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        SectionHeading(stringResource(Res.string.feature_send_money_creditor_heading))
        if (state.hasBeneficiaries) {
            SendMoneyPickerList(
                rows = state.beneficiaries,
                selectedId = state.creditor?.beneficiaryId,
                onSelect = { onAction(SendMoneyAction.SelectCreditor(it)) },
                tagFor = SendMoneyTestTags::creditorRow,
                selectedLabel = stringResource(Res.string.feature_send_money_selected_a11y),
                modifier = Modifier.testTag(SendMoneyTestTags.CREDITOR_LIST),
            )
        } else {
            NoSavedPayees()
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

        OutlinedTextField(
            value = state.reference,
            onValueChange = { onAction(SendMoneyAction.EnterReference(it.take(REFERENCE_MAX_LENGTH))) },
            label = { Text(stringResource(Res.string.feature_send_money_reference_label)) },
            supportingText = { Text(stringResource(Res.string.feature_send_money_reference_helper)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.REFERENCE_FIELD),
        )
    }
}

/**
 * The last thing the customer reads before money moves, so it is built around one figure.
 *
 * The amount leads at display size with the payee immediately under it: everything else on this
 * step is detail that confirms *that* statement rather than competing with it.
 *
 * Deliberately absent is any fee or debit total. Charges arrive on the consent response, which does
 * not exist until Confirm is tapped, so a figure here could only be hardcoded — and the sandbox
 * quotes charges on some payments, which would make a printed £0.00 untrue on the one screen where
 * that matters most.
 */
@Composable
private fun SendMoneyReviewPage(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val payerLabel = state.debtorAccountRow?.let { row ->
        accountDisplayName(
            nickname = row.nickname,
            accountSubType = row.accountSubType,
            accountNumber = row.accountNumber,
            rawIdentification = row.rawIdentification,
        )
    }.orEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SendMoneyTestTags.REVIEW_PAGE)
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        ReviewHero(amountLabel = state.amountLabel, creditorName = state.creditorLabel)

        Column(
            modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.REVIEW_SUMMARY),
            verticalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            SectionHeading(stringResource(Res.string.feature_send_money_review_details_heading))
            ReviewRow(
                label = stringResource(Res.string.feature_send_money_review_to),
                value = state.creditorLabel,
                tag = SendMoneyTestTags.REVIEW_TO,
                secondary = state.creditorSupporting,
            )
            ReviewRow(
                label = stringResource(Res.string.feature_send_money_review_from),
                value = payerLabel,
                tag = SendMoneyTestTags.REVIEW_FROM,
            )
            ReviewRow(
                label = stringResource(Res.string.feature_send_money_review_reference),
                value = state.reference.ifBlank {
                    stringResource(Res.string.feature_send_money_review_reference_empty)
                },
                tag = SendMoneyTestTags.REVIEW_REFERENCE,
            )
            // True of the instruction being sent: every Initiation declares LocalInstrument
            // UK.OBIE.FPS. It says which rail, not how fast — the bank promises no timing.
            ReviewRow(
                label = stringResource(Res.string.feature_send_money_review_sent_via),
                value = stringResource(Res.string.feature_send_money_review_sent_via_value),
                tag = SendMoneyTestTags.REVIEW_SENT_VIA,
            )
            ReviewRow(
                label = stringResource(Res.string.feature_send_money_review_total),
                value = state.amountLabel,
                tag = SendMoneyTestTags.REVIEW_TOTAL,
                emphasis = true,
            )
        }

        AuthorisationNotice()

        MifosFilledPillButton(
            label = stringResource(Res.string.feature_send_money_confirm, state.amountLabel),
            onClick = { onAction(SendMoneyAction.ConfirmAndStageConsent) },
            icon = Icons.Filled.Lock,
            testTag = SendMoneyTestTags.CONFIRM_BUTTON,
        )
        MifosTonalPillButton(
            label = stringResource(Res.string.feature_send_money_edit_payment),
            onClick = { onAction(SendMoneyAction.BackStep) },
            testTag = SendMoneyTestTags.EDIT_PAYMENT_BUTTON,
        )
    }
}

/** The amount, at the size it deserves, with who it is going to directly beneath. */
@Composable
private fun ReviewHero(amountLabel: String, creditorName: String) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.REVIEW_HERO),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeroGap),
    ) {
        Text(
            text = amountLabel,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag(SendMoneyTestTags.REVIEW_AMOUNT),
        )
        Text(
            text = stringResource(Res.string.feature_send_money_review_hero_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (creditorName.isNotBlank()) {
            PayeeChip(creditorName)
        }
    }
}

/** Initials beside the name, reusing the same derivation the payee picker rows use. */
@Composable
private fun PayeeChip(creditorName: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(ChipCorner))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = ChipPaddingHorizontal, vertical = ChipPaddingVertical)
            .testTag(SendMoneyTestTags.REVIEW_PAYEE_CHIP),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        Box(
            modifier = Modifier
                .size(ChipAvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsOf(creditorName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = creditorName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Says what happens next, rather than asserting a check this app has not made.
 *
 * The original mockup carried "Confirmation of Payee passed" here. HSBC exposes no CoP result to
 * this app, so claiming one would be inventing a reassurance. What *is* true, and worth saying
 * before an irreversible action, is that nothing moves until the bank has authenticated the PSU.
 */
@Composable
private fun AuthorisationNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NoticeCorner))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(NoticePadding)
            .testTag(SendMoneyTestTags.REVIEW_AUTH_NOTICE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NoticeGap),
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(NoticeIconSize),
        )
        Text(
            text = stringResource(Res.string.feature_send_money_review_auth_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ReviewRow(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
    secondary: String = "",
    emphasis: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth().testTag(tag)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasis) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (secondary.isNotBlank()) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text.uppercase(),
        style = KptTheme.typography.bodySmall,
        color = KptTheme.colorScheme.outline,
    )
}

private fun SendMoneyAmountProblem.messageResource(): StringResource = when (this) {
    SendMoneyAmountProblem.NotANumber -> Res.string.feature_send_money_amount_error_not_a_number
    SendMoneyAmountProblem.NotPositive -> Res.string.feature_send_money_amount_error_not_positive
    SendMoneyAmountProblem.ExceedsAvailableBalance -> Res.string.feature_send_money_amount_error_exceeds_balance
}
