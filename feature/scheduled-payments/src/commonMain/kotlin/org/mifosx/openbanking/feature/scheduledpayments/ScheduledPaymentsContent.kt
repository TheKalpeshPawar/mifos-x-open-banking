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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentType
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.Res
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_account_prefix
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_card_a11y
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_due_prefix
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_list_a11y
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_ref_prefix
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_type_arrival
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_type_arrival_a11y
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_type_execution
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_type_execution_a11y
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_type_unknown
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_type_unknown_a11y
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentUiModel

private val SCREEN_PADDING = 16.dp
private val CARD_GAP = 12.dp
private val CARD_RADIUS = 12.dp
private val CARD_ELEVATION = 1.dp
private val CARD_PADDING = 16.dp
private val CARD_LINE_GAP = 8.dp
private val HEADER_GAP = 8.dp
private val META_GAP = 4.dp
private val META_TOP_PADDING = 4.dp
private val CHIP_RADIUS = 999.dp
private val CHIP_GAP = 4.dp
private val CHIP_HORIZONTAL_PADDING = 12.dp
private val CHIP_VERTICAL_PADDING = 6.dp
private val CHIP_ICON_SIZE = 18.dp

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
        contentPadding = PaddingValues(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(CARD_GAP),
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
 * One scheduled payment: payee and amount, the due date, a type chip and the destination meta.
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
        shape = RoundedCornerShape(CARD_RADIUS),
        modifier = modifier
            .fillMaxWidth()
            .testTag(ScheduledPaymentsTestTags.card(payment.scheduledPaymentId))
            .semantics { contentDescription = cardDescription },
    ) {
        Column(
            modifier = Modifier.padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(CARD_LINE_GAP),
        ) {
            CardHeader(payment = payment)
            Text(
                text = "${stringResource(Res.string.feature_scheduled_payments_due_prefix)} " +
                    payment.scheduledDateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TypeChip(payment = payment)
            CardMeta(payment = payment)
        }
    }
}

@Composable
private fun CardHeader(payment: ScheduledPaymentUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HEADER_GAP),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = payment.payeeName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = payment.amountLabel,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * The ScheduledType chip: a tonal secondary-container assist chip whose icon, label and
 * accessibility phrasing are resolved here from the [ScheduledPaymentType] the view model carried
 * through. An unrecognised type still renders, with a neutral schedule glyph and label.
 */
@Composable
private fun TypeChip(payment: ScheduledPaymentUiModel, modifier: Modifier = Modifier) {
    val presentation = payment.scheduledType.chipPresentation()
    val description = stringResource(presentation.accessibility)
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(CHIP_RADIUS),
        modifier = modifier
            .testTag(ScheduledPaymentsTestTags.typeChip(payment.scheduledPaymentId))
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = CHIP_HORIZONTAL_PADDING,
                vertical = CHIP_VERTICAL_PADDING,
            ),
            horizontalArrangement = Arrangement.spacedBy(CHIP_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = null,
                modifier = Modifier.size(CHIP_ICON_SIZE),
            )
            Text(
                text = stringResource(presentation.label),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun CardMeta(payment: ScheduledPaymentUiModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(META_GAP),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MetaRow(
            prefix = stringResource(Res.string.feature_scheduled_payments_account_prefix),
            value = payment.creditorIdentification,
            monospace = true,
            topPadding = true,
        )
        MetaRow(
            prefix = stringResource(Res.string.feature_scheduled_payments_ref_prefix),
            value = payment.reference,
            monospace = false,
            topPadding = false,
        )
    }
}

@Composable
private fun MetaRow(prefix: String, value: String, monospace: Boolean, topPadding: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (topPadding) META_TOP_PADDING else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(META_GAP),
    ) {
        Text(
            text = prefix,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.labelSmall
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The icon, label and accessibility strings a [ScheduledPaymentType] renders as in the chip. */
private data class ChipPresentation(
    val icon: ImageVector,
    val label: StringResource,
    val accessibility: StringResource,
)

private fun ScheduledPaymentType.chipPresentation(): ChipPresentation = when (this) {
    ScheduledPaymentType.Execution -> ChipPresentation(
        icon = Icons.Outlined.CalendarToday,
        label = Res.string.feature_scheduled_payments_type_execution,
        accessibility = Res.string.feature_scheduled_payments_type_execution_a11y,
    )
    ScheduledPaymentType.Arrival -> ChipPresentation(
        icon = Icons.Outlined.ArrowDownward,
        label = Res.string.feature_scheduled_payments_type_arrival,
        accessibility = Res.string.feature_scheduled_payments_type_arrival_a11y,
    )
    ScheduledPaymentType.Unknown -> ChipPresentation(
        icon = Icons.Outlined.Schedule,
        label = Res.string.feature_scheduled_payments_type_unknown,
        accessibility = Res.string.feature_scheduled_payments_type_unknown_a11y,
    )
}
