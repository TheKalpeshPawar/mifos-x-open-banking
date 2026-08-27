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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
import org.mifosx.openbanking.core.ui.account.MifosAccountReviewRow
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.core.ui.components.MifosTonalPillButton
import org.mifosx.openbanking.core.ui.payee.initialsOf
import org.mifosx.openbanking.feature.sendmoney.components.chargeBearerLabel
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_confirm
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_edit_payment
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_auth_notice
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_charge_bearer
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_details_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_from
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_from_bank_choice
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_heading
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_hero_caption
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_reference
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_reference_empty
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_sent_via
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_sent_via_domestic
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_sent_via_international
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_to
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_review_total
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAction
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyUiState
import template.core.base.designsystem.theme.KptTheme

/**
 * The last thing the customer reads before money moves.
 *
 * Split from [SendMoneyContent] because it is the other page of the form, and the two had grown far
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
internal fun SendMoneyReviewPage(
    state: SendMoneyUiState.Content,
    onAction: (SendMoneyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SendMoneyTestTags.REVIEW_PAGE)
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        // The page lead. It earns a line of its own now that Review is a page rather than a step
        // behind an indicator that already said where the customer was.
        Text(
            text = stringResource(Res.string.feature_send_money_review_heading),
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(SendMoneyTestTags.REVIEW_LEAD),
        )

        ReviewHero(amountLabel = state.amountLabel, creditorName = state.creditorLabel)

        Column(
            modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.REVIEW_SUMMARY),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            SectionHeading(stringResource(Res.string.feature_send_money_review_details_heading))
            ReviewRow(
                label = stringResource(Res.string.feature_send_money_review_to),
                value = state.creditorLabel,
                tag = SendMoneyTestTags.REVIEW_TO,
                secondary = state.creditorSupporting,
            )
            MifosAccountReviewRow(
                label = stringResource(Res.string.feature_send_money_review_from),
                account = state.debtorAccountRow?.account,
                absentLabel = stringResource(Res.string.feature_send_money_review_from_bank_choice),
                modifier = Modifier.testTag(SendMoneyTestTags.REVIEW_FROM),
            )
            if (state.rail == PaymentRail.Domestic) {
                ReviewRow(
                    label = stringResource(Res.string.feature_send_money_review_reference),
                    value = state.reference.ifBlank {
                        stringResource(Res.string.feature_send_money_review_reference_empty)
                    },
                    tag = SendMoneyTestTags.REVIEW_REFERENCE,
                )
            } else {
                // No "Recipient receives" row. It existed because the instructed currency and the
                // currency of transfer could differ; they no longer can — the transfer currency is
                // derived from the amount's — so the row restated the currency already carried by
                // the figure beside it. `formatMinorUnits` renders GBP, EUR and USD as £/€/$ and
                // every other code as the code itself, so the amount names its own currency.
                ReviewRow(
                    label = stringResource(Res.string.feature_send_money_review_charge_bearer),
                    value = chargeBearerLabel(state.chargeBearer),
                    tag = SendMoneyTestTags.REVIEW_CHARGE_BEARER,
                )
            }
            // Which rail, not how fast — the bank promises no timing. Derived rather than fixed:
            // it read "Faster Payments" on both, which is untrue of an international payment.
            ReviewRow(
                label = stringResource(Res.string.feature_send_money_review_sent_via),
                value = stringResource(state.rail.sentViaLabel()),
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
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = amountLabel,
            style = KptTheme.typography.displaySmall,
            color = KptTheme.colorScheme.primary,
            modifier = Modifier.testTag(SendMoneyTestTags.REVIEW_AMOUNT),
        )
        Text(
            text = stringResource(Res.string.feature_send_money_review_hero_caption),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
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
            .clip(KptTheme.shapes.large)
            .background(KptTheme.colorScheme.surfaceVariant)
            .padding(horizontal = KptTheme.spacing.sm, vertical = KptTheme.spacing.xs)
            .testTag(SendMoneyTestTags.REVIEW_PAYEE_CHIP),
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
            .testTag(SendMoneyTestTags.REVIEW_AUTH_NOTICE),
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
            text = stringResource(Res.string.feature_send_money_review_auth_notice),
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

/** Which rail carried it. Not a speed claim — the bank commits to no timing on either. */
private fun PaymentRail.sentViaLabel(): StringResource = when (this) {
    PaymentRail.Domestic -> Res.string.feature_send_money_review_sent_via_domestic
    PaymentRail.International -> Res.string.feature_send_money_review_sent_via_international
}
