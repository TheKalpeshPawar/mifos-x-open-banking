/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.feature.standingorders.generated.resources.Res
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_amount_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_card_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_final_payment
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_frequency_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_list_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_next_payment
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_payee_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_reference
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_sort_code_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_status_accessibility
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrderRowUi
import template.core.base.designsystem.theme.KptTheme

private const val INACTIVE_CARD_ALPHA = 0.72f

/**
 * The standing-orders body: a lazy list of one order card per instruction.
 *
 * The list is lazy because an account's order count is bank-decided and unbounded — OBIE returns
 * the full set in one unpaginated response, so the composition, not the request, is where this is
 * kept cheap.
 */
@Composable
internal fun StandingOrdersContent(
    orders: List<StandingOrderRowUi>,
    modifier: Modifier = Modifier,
) {
    val listDescription = stringResource(Res.string.feature_standing_orders_list_accessibility)
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(StandingOrdersTestTags.CONTENT)
            .semantics { contentDescription = listDescription },
        contentPadding = PaddingValues(KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        items(items = orders, key = { it.standingOrderId.ifBlank { it.payeeName } }) { order ->
            StandingOrderCard(order = order)
        }
    }
}

/**
 * One standing order: payee, status badge, next amount, frequency, dates, destination account and
 * reference.
 *
 * An inactive order is dimmed rather than hidden — a cancelled instruction is information the user
 * came here for. The amount stays neutral on-surface, never the error colour: a standing order is
 * an expected outgoing, not a failure.
 */
@Composable
private fun StandingOrderCard(order: StandingOrderRowUi, modifier: Modifier = Modifier) {
    val cardDescription = stringResource(
        Res.string.feature_standing_orders_card_accessibility,
        order.payeeName,
        order.statusLabel,
    )
    Surface(
        color = KptTheme.colorScheme.surfaceContainer,
        shape = KptTheme.shapes.medium,
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (order.isActive) 1f else INACTIVE_CARD_ALPHA)
            .testTag(StandingOrdersTestTags.card(order.standingOrderId))
            .semantics { contentDescription = cardDescription },
    ) {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            StandingOrderCardHeader(order = order)
            StandingOrderAmount(order = order)
            StandingOrderMetaLines(order = order)
        }
    }
}

@Composable
private fun StandingOrderCardHeader(order: StandingOrderRowUi, modifier: Modifier = Modifier) {
    val payeeDescription = stringResource(
        Res.string.feature_standing_orders_payee_accessibility,
        order.payeeName,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = order.payeeName,
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { contentDescription = payeeDescription },
        )
        StatusBadge(order = order)
    }
}

/**
 * Active orders get the filled primary-container badge; everything else gets the secondary
 * container.
 *
 * A filled secondary rather than direct debits' outline: this screen's inactive rows already carry
 * a dimmed card, so an outline badge on top of that reads as absent rather than merely quieter,
 * and the spec's `Inactive -> secondary` mapping says the status should still be legible.
 */
@Composable
private fun StatusBadge(order: StandingOrderRowUi, modifier: Modifier = Modifier) {
    val description = stringResource(
        Res.string.feature_standing_orders_status_accessibility,
        order.statusLabel,
    )
    Surface(
        color = if (order.isActive) {
            KptTheme.colorScheme.primaryContainer
        } else {
            KptTheme.colorScheme.secondaryContainer
        },
        shape = DesignToken.shapes.pill,
        modifier = modifier
            .testTag(StandingOrdersTestTags.statusBadge(order.standingOrderId))
            .semantics { contentDescription = description },
    ) {
        Text(
            text = order.statusLabel,
            style = KptTheme.typography.labelMedium,
            color = if (order.isActive) {
                KptTheme.colorScheme.onPrimaryContainer
            } else {
                KptTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.padding(
                horizontal = KptTheme.spacing.sm,
                vertical = KptTheme.spacing.xs,
            ),
        )
    }
}

/**
 * The next payment amount in monospace, with the ISO currency code set smaller beside it.
 *
 * Monospace so figures line up column-wise down the list, which is what makes two amounts
 * comparable at a glance. The currency sits in its own smaller run rather than inside the amount
 * string so it reads as a unit label, not another digit group.
 */
@Composable
private fun StandingOrderAmount(order: StandingOrderRowUi, modifier: Modifier = Modifier) {
    if (order.amountLabel.isBlank()) return
    val description = stringResource(
        Res.string.feature_standing_orders_amount_accessibility,
        order.amountLabel,
        order.currencyLabel,
    )
    Row(
        modifier = modifier
            .testTag(StandingOrdersTestTags.amount(order.standingOrderId))
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = order.amountLabel,
            style = KptTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
            color = if (order.isActive) {
                KptTheme.colorScheme.onSurface
            } else {
                KptTheme.colorScheme.onSurfaceVariant
            },
        )
        if (order.currencyLabel.isNotBlank()) {
            Text(
                text = order.currencyLabel,
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Frequency, the payment dates, then a rule separating those from the destination account and
 * reference.
 *
 * The divider is what turns the card from a list of seven similar lines into two groups: when the
 * money moves, and where it goes. Each line is omitted when the bank sent nothing for it — a label
 * with no value tells the user less than its absence does.
 */
@Composable
private fun StandingOrderMetaLines(order: StandingOrderRowUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs)) {
        StandingOrderScheduleLines(order = order)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = KptTheme.spacing.xs),
            thickness = DesignToken.strokes.hairline,
            color = KptTheme.colorScheme.outlineVariant,
        )
        StandingOrderDestinationLines(order = order)
    }
}

@Composable
private fun StandingOrderScheduleLines(order: StandingOrderRowUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs)) {
        if (order.frequencyLabel.isNotBlank()) {
            MetaLine(
                label = stringResource(Res.string.feature_standing_orders_frequency_accessibility),
                value = order.frequencyLabel,
                testTag = StandingOrdersTestTags.frequency(order.standingOrderId),
            )
        }
        if (order.nextDateLabel.isNotBlank()) {
            MetaLine(
                label = stringResource(Res.string.feature_standing_orders_next_payment),
                value = order.nextDateLabel,
                testTag = StandingOrdersTestTags.nextDate(order.standingOrderId),
            )
        }
        if (order.hasFinalPayment) {
            MetaLine(
                label = stringResource(Res.string.feature_standing_orders_final_payment),
                value = order.finalDateLabel,
                testTag = StandingOrdersTestTags.finalDate(order.standingOrderId),
            )
        }
    }
}

@Composable
private fun StandingOrderDestinationLines(order: StandingOrderRowUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs)) {
        if (order.sortCodeLabel.isNotBlank()) {
            MetaLine(
                label = stringResource(Res.string.feature_standing_orders_sort_code_accessibility),
                value = order.sortCodeLabel,
                testTag = StandingOrdersTestTags.sortCode(order.standingOrderId),
                monospace = true,
            )
        }
        if (order.referenceLabel.isNotBlank()) {
            MetaLine(
                label = stringResource(Res.string.feature_standing_orders_reference),
                value = order.referenceLabel,
                testTag = StandingOrdersTestTags.reference(order.standingOrderId),
            )
        }
    }
}

/**
 * One label + value line on a card. [monospace] renders the value in fixed-width digits for the
 * identification line, where the grouping stays readable as an account identifier.
 */
@Composable
private fun MetaLine(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = KptTheme.spacing.xs)
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = label,
            style = KptTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (monospace) {
                KptTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                KptTheme.typography.titleSmall
            },
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}
