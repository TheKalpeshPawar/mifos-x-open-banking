/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentsschedulepayment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.banking.payment.CreditorSelection
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
import org.mifosx.openbanking.core.model.banking.toAccountScheme
import org.mifosx.openbanking.core.ui.account.MifosAccountReviewRow
import org.mifosx.openbanking.core.ui.account.MifosPartyReviewRow
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.core.ui.components.MifosTonalPillButton
import org.mifosx.openbanking.core.ui.payee.initialsOf
import org.mifosx.openbanking.feature.paymentsschedulepayment.components.chargeBearerLabel
import org.mifosx.openbanking.feature.paymentsschedulepayment.components.currencyName
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.Res
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_auth_notice
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_charge_bearer
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_charge_caveat
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_confirm
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_details_heading
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_edit
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_from
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_from_bank_choice
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_heading
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_not_yet_made_body
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_not_yet_made_title
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_reference
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_reference_empty
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_scheduled_for
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_sent_as
import org.mifosx.openbanking.feature.paymentsschedulepayment.generated.resources.feature_payments_schedule_payment_review_to
import org.mifosx.openbanking.feature.paymentsschedulepayment.ui.SchedulePaymentAction
import org.mifosx.openbanking.feature.paymentsschedulepayment.ui.SchedulePaymentStep
import org.mifosx.openbanking.feature.paymentsschedulepayment.ui.SchedulePaymentUiState
import template.core.base.designsystem.theme.KptTheme

/**
 * The last thing the customer reads before money moves.
 *
 * Split from [SchedulePaymentContent] because it is the other page of the form, and the two had grown far
 * enough apart that one file was describing two screens.
 */

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
internal fun SchedulePaymentReviewPage(
    state: SchedulePaymentUiState.Content,
    onAction: (SchedulePaymentAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SchedulePaymentTestTags.REVIEW_PAGE)
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        // The page lead. It earns a line of its own now that Review is a page rather than a step
        // behind an indicator that already said where the customer was.
        Text(
            text = stringResource(Res.string.feature_payments_schedule_payment_review_heading),
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(SchedulePaymentTestTags.REVIEW_LEAD),
        )

        ReviewHero(
            dateLabel = state.executionDateLabel,
            amountLabel = state.amountLabel,
            creditorName = state.creditorLabel,
        )

        Column(
            modifier = Modifier.fillMaxWidth().testTag(SchedulePaymentTestTags.REVIEW_SUMMARY),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            SectionHeading(stringResource(Res.string.feature_payments_schedule_payment_review_details_heading))
            // Blank when the PSU left the choice to the bank — which is a decision they made, so it
            // is stated rather than left as an empty row that reads like a missing field.
            MifosAccountReviewRow(
                label = stringResource(Res.string.feature_payments_schedule_payment_review_from),
                account = state.debtorAccountRow?.account,
                absentLabel = stringResource(
                    Res.string.feature_payments_schedule_payment_review_from_bank_choice,
                ),
                modifier = Modifier.testTag(SchedulePaymentTestTags.REVIEW_FROM),
            )
            MifosPartyReviewRow(
                label = stringResource(Res.string.feature_payments_schedule_payment_review_to),
                name = state.creditorLabel,
                scheme = state.creditor?.scheme?.toAccountScheme() ?: AccountScheme.Other,
                identification = state.creditor?.identification.orEmpty(),
                modifier = Modifier.testTag(SchedulePaymentTestTags.REVIEW_TO),
            )
            if (state.rail == PaymentRail.Domestic) {
                ReviewRow(
                    label = stringResource(Res.string.feature_payments_schedule_payment_review_reference),
                    value = state.reference.ifBlank {
                        stringResource(Res.string.feature_payments_schedule_payment_review_reference_empty)
                    },
                    tag = SchedulePaymentTestTags.REVIEW_REFERENCE,
                )
            } else {
                // No "Recipient receives" row. It existed because the instructed currency and the
                // currency of transfer could differ; they no longer can — the transfer currency is
                // derived from the amount's — so the row restated the currency already carried by
                // the figure beside it. `formatMinorUnits` renders GBP, EUR and USD as £/€/$ and
                // every other code as the code itself, so the amount names its own currency.
                ReviewRow(
                    label = stringResource(Res.string.feature_payments_schedule_payment_review_sent_as),
                    value = currencyName(state.instructedCurrency),
                    tag = SchedulePaymentTestTags.REVIEW_SENT_AS,
                )
                // The bearer, and deliberately no figure. This rail declares its charge only when
                // the payment resource is created — which is after the customer has authorised — so
                // any number here would be invented. The caveat below says so in as many words.
                ReviewRow(
                    label = stringResource(Res.string.feature_payments_schedule_payment_review_charge_bearer),
                    value = chargeBearerLabel(state.chargeBearer),
                    tag = SchedulePaymentTestTags.REVIEW_CHARGE_ROW,
                )
            }
        }

        if (state.rail == PaymentRail.International) {
            Text(
                text = stringResource(Res.string.feature_payments_schedule_payment_review_charge_caveat),
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(SchedulePaymentTestTags.REVIEW_CHARGE_CAVEAT),
            )
        }

        NotYetMadeNotice(dateLabel = state.executionDateLabel)

        AuthorisationNotice()

        MifosFilledPillButton(
            label = stringResource(Res.string.feature_payments_schedule_payment_review_confirm),
            onClick = { onAction(SchedulePaymentAction.ConfirmAndStageConsent) },
            icon = Icons.Filled.Lock,
            testTag = SchedulePaymentTestTags.CONFIRM_BUTTON,
        )
        MifosTonalPillButton(
            label = stringResource(Res.string.feature_payments_schedule_payment_review_edit),
            onClick = { onAction(SchedulePaymentAction.BackStep) },
            testTag = SchedulePaymentTestTags.EDIT_PAYMENT_BUTTON,
        )
    }
}

/**
 * The date first, then the amount, then who it is going to.
 *
 * The order is the difference between this screen and the immediate rail's. There, the amount is the
 * whole statement. Here the customer is confirming *when* as much as *how much* — a payment they will
 * not be able to cancel once approved — so the date leads and the amount follows it.
 */
@Composable
private fun ReviewHero(dateLabel: String, amountLabel: String, creditorName: String) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(SchedulePaymentTestTags.REVIEW_HERO),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.feature_payments_schedule_payment_review_scheduled_for),
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = dateLabel,
            style = KptTheme.typography.titleLarge,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(SchedulePaymentTestTags.REVIEW_DATE_ROW),
        )
        Text(
            text = amountLabel,
            style = KptTheme.typography.displaySmall,
            color = KptTheme.colorScheme.primary,
            modifier = Modifier.testTag(SchedulePaymentTestTags.REVIEW_AMOUNT),
        )
        if (creditorName.isNotBlank()) {
            PayeeChip(creditorName)
        }
    }
}

/**
 * The panel that refuses to overstate what has happened.
 *
 * Every word here is chosen against a specific way this screen could mislead. It does not say paid,
 * sent or complete, because nothing has moved and nothing will until the date. It names the date
 * again, because that is the commitment. And it says the payment cannot be cancelled from this app —
 * which is not a limitation of this build but of the bank: a scheduled consent answers `405` to a
 * delete and survives it, so an in-app cancel could never be honest.
 *
 * The only tinted surface on the page, so it reads as the caveat rather than as one more detail row.
 */
@Composable
private fun NotYetMadeNotice(dateLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(KptTheme.shapes.medium)
            .background(KptTheme.colorScheme.primaryContainer)
            .padding(KptTheme.spacing.md)
            .testTag(SchedulePaymentTestTags.REVIEW_NOT_YET_MADE),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        Text(
            text = stringResource(Res.string.feature_payments_schedule_payment_review_not_yet_made_title),
            style = KptTheme.typography.titleSmall,
            color = KptTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(
                Res.string.feature_payments_schedule_payment_review_not_yet_made_body,
                dateLabel,
            ),
            style = KptTheme.typography.bodySmall,
            color = KptTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Initials beside the name, reusing the same derivation the payee picker rows use. */
@Composable
private fun PayeeChip(creditorName: String) {
    Row(
        modifier = Modifier
            .clip(KptTheme.shapes.large)
            .background(KptTheme.colorScheme.surfaceVariant)
            .padding(horizontal = KptTheme.spacing.sm, vertical = KptTheme.spacing.xs)
            .testTag(SchedulePaymentTestTags.REVIEW_PAYEE_CHIP),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(DesignToken.sizes.iconMedium)
                .clip(CircleShape)
                .background(KptTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsOf(creditorName),
                style = KptTheme.typography.labelSmall,
                color = KptTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = creditorName,
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurface,
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
            .clip(KptTheme.shapes.medium)
            .background(KptTheme.colorScheme.secondaryContainer)
            .padding(KptTheme.spacing.md)
            .testTag(SchedulePaymentTestTags.REVIEW_AUTH_NOTICE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = KptTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(DesignToken.sizes.iconExtraSmall),
        )
        Text(
            text = stringResource(Res.string.feature_payments_schedule_payment_review_auth_notice),
            style = KptTheme.typography.bodySmall,
            color = KptTheme.colorScheme.onSecondaryContainer,
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
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasis) {
                KptTheme.typography.headlineSmall
            } else {
                KptTheme.typography.bodyLarge
            },
            color = KptTheme.colorScheme.onSurface,
        )
        if (secondary.isNotBlank()) {
            Text(
                text = secondary,
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun SchedulePaymentReviewPagePreview() {
    MifosXOpenBankingTheme {
        SchedulePaymentReviewPage(
            state = SchedulePaymentUiState.Content(
                step = SchedulePaymentStep.Review,
                debtorAccounts = emptyList(),
                beneficiaries = emptyList(),
                debtorRows = emptyList(),
                creditor = CreditorSelection(
                    name = "Mr Nico",
                    scheme = BeneficiaryScheme.SortCode,
                    identification = "80200110203349",
                ),
                creditorLabel = "Mr Nico",
                debtorAccountRow = AccountWithBalance(
                    account = BankAccount(
                        accountId = "1123456841",
                        accountTypeCode = "CACC",
                        currency = "GBP",
                        identification = "80200110203348",
                        scheme = AccountScheme.SortCode,
                        description = "Description of the account",
                        accountHolderName = "Mr Robert",
                    ),
                    balance = null,
                ),
                amountLabel = "£6.00",
                reference = "BFRS.RFRNC.546",
                today = LocalDate(2026, 8, 25),
            ),
            onAction = {},
        )
    }
}
