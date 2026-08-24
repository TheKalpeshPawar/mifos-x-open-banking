/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.scheduledpayments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.Res
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_account_prefix
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_card_a11y
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_due_prefix
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_list_a11y
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentUiModel
import template.core.base.designsystem.theme.KptTheme

/**
 * The scheduled-payments body: a lazy list of one card per future-dated instruction.
 *
 * The list is lazy because an account's scheduled-payment count is bank-decided and unbounded —
 * OBIE returns the full set in one unpaginated response, so the composition, not the request, is
 * where this is kept cheap.
 */
@Composable
internal fun ScheduledPaymentsContent(
    payments: List<ScheduledPaymentUiModel>,
    modifier: Modifier = Modifier,
) {
    val listDescription = stringResource(Res.string.feature_scheduled_payments_list_a11y)
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ScheduledPaymentsTestTags.CONTENT_LIST)
            .semantics { contentDescription = listDescription },
        contentPadding = PaddingValues(KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        items(
            items = payments,
            key = { it.scheduledPaymentId.ifBlank { it.payeeName } },
        ) { payment ->
            ScheduledPaymentCard(payment = payment)
        }
    }
}

/**
 * One scheduled payment: payee and amount, the due date and the destination meta.
 *
 * The amount takes the error colour and a monospace face: a future-dated outgoing is the money the
 * user is being told will leave, and the design foregrounds it as such. The card carries no click
 * handler — it is a read-only readout under AISP consent, with nothing to drill into.
 */
@Composable
private fun ScheduledPaymentCard(payment: ScheduledPaymentUiModel, modifier: Modifier = Modifier) {
    val cardDescription = stringResource(
        Res.string.feature_scheduled_payments_card_a11y,
        payment.amountLabel,
        payment.payeeName,
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = KptTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = KptTheme.elevation.level0),
        shape = KptTheme.shapes.medium,
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag(ScheduledPaymentsTestTags.card(payment.scheduledPaymentId))
            .semantics { contentDescription = cardDescription },
    ) {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            CardHeader(payment = payment)
            Text(
                text = "${stringResource(Res.string.feature_scheduled_payments_due_prefix)} " +
                    payment.scheduledDateLabel,
                style = KptTheme.typography.titleSmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
            CardMeta(payment = payment)
        }
    }
}

@Composable
private fun CardHeader(payment: ScheduledPaymentUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = payment.payeeName,
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = payment.amountLabel,
            style = KptTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = KptTheme.colorScheme.error,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CardMeta(payment: ScheduledPaymentUiModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(top = KptTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.feature_scheduled_payments_account_prefix),
                style = KptTheme.typography.titleSmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = payment.creditorIdentification,
                style = KptTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.feature_scheduled_payments_account_prefix),
                style = KptTheme.typography.titleSmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = payment.reference,
                style = KptTheme.typography.titleSmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
