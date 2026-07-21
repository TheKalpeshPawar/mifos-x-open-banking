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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.standingorders.generated.resources.Res
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_amount_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_card_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_final_payment
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_final_payment_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_frequency_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_list_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_next_payment
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_next_payment_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_payee_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_reference
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_reference_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_sort_code_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_status_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_summary
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_summary_accessibility
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrderRowUi

private val SCREEN_PADDING = 16.dp
private val CARD_GAP = 12.dp
private val CARD_RADIUS = 12.dp
private val CARD_PADDING = 16.dp
private val CARD_LINE_GAP = 8.dp
private val META_LINE_GAP = 4.dp
private val DIVIDER_MARGIN = 4.dp
private val DIVIDER_THICKNESS = 1.dp
private val BADGE_HORIZONTAL_PADDING = 8.dp
private val BADGE_VERTICAL_PADDING = 2.dp
private val AMOUNT_CURRENCY_GAP = 6.dp
private val PILL_RADIUS = 999.dp
private val AMOUNT_SIZE = 24.sp
private val CURRENCY_SIZE = 14.sp
private val SORT_CODE_SIZE = 12.sp
private const val INACTIVE_CARD_ALPHA = 0.72f

/**
 * The standing-orders body: the summary line above the order list.
 *
 * The list is lazy because an account's order count is bank-decided and unbounded — OBIE returns
 * the full set in one unpaginated response, so the composition, not the request, is where this is
 * kept cheap.
 */
@Composable
internal fun StandingOrdersContent(
    orders: List<StandingOrderRowUi>,
    activeCount: Int,
    inactiveCount: Int,
    modifier: Modifier = Modifier,
) {
    val listDescription = stringResource(Res.string.feature_standing_orders_list_accessibility)
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(StandingOrdersTestTags.CONTENT)
            .semantics { contentDescription = listDescription },
        contentPadding = PaddingValues(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(CARD_GAP),
    ) {
        item {
            SummaryRow(activeCount = activeCount, inactiveCount = inactiveCount)
        }
        items(items = orders, key = { it.standingOrderId.ifBlank { it.payeeName } }) { order ->
            StandingOrderCard(order = order)
        }
    }
}

/**
 * The `4 Active · 1 Inactive` readout.
 *
 * A single line rather than the pair of chips direct debits uses. The counts here are context for
 * the list below, not a control surface, and one sentence says that more plainly than two pills
 * that look tappable.
 */
@Composable
private fun SummaryRow(
    activeCount: Int,
    inactiveCount: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        Res.string.feature_standing_orders_summary_accessibility,
        activeCount,
        inactiveCount,
    )
    Text(
        text = stringResource(Res.string.feature_standing_orders_summary, activeCount, inactiveCount),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .testTag(StandingOrdersTestTags.SUMMARY_ROW)
            .semantics { contentDescription = description },
    )
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
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(CARD_RADIUS),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (order.isActive) 1f else INACTIVE_CARD_ALPHA)
            .testTag(StandingOrdersTestTags.card(order.standingOrderId))
            .semantics { contentDescription = cardDescription },
    ) {
        Column(
            modifier = Modifier.padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(CARD_LINE_GAP),
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
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
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        shape = RoundedCornerShape(PILL_RADIUS),
        modifier = modifier
            .testTag(StandingOrdersTestTags.statusBadge(order.standingOrderId))
            .semantics { contentDescription = description },
    ) {
        Text(
            text = order.statusLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (order.isActive) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.padding(
                horizontal = BADGE_HORIZONTAL_PADDING,
                vertical = BADGE_VERTICAL_PADDING,
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
        horizontalArrangement = Arrangement.spacedBy(AMOUNT_CURRENCY_GAP),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = order.amountLabel,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = AMOUNT_SIZE,
            ),
            color = if (order.isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (order.currencyLabel.isNotBlank()) {
            Text(
                text = order.currencyLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = CURRENCY_SIZE),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(META_LINE_GAP)) {
        StandingOrderScheduleLines(order = order)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = DIVIDER_MARGIN),
            thickness = DIVIDER_THICKNESS,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        StandingOrderDestinationLines(order = order)
    }
}

@Composable
private fun StandingOrderScheduleLines(order: StandingOrderRowUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(META_LINE_GAP)) {
        if (order.frequencyLabel.isNotBlank()) {
            MetaLine(
                text = order.frequencyLabel,
                description = stringResource(
                    Res.string.feature_standing_orders_frequency_accessibility,
                    order.frequencyLabel,
                ),
                testTag = StandingOrdersTestTags.frequency(order.standingOrderId),
            )
        }
        if (order.nextDateLabel.isNotBlank()) {
            MetaLine(
                text = stringResource(
                    Res.string.feature_standing_orders_next_payment,
                    order.nextDateLabel,
                ),
                description = stringResource(
                    Res.string.feature_standing_orders_next_payment_accessibility,
                    order.nextDateLabel,
                ),
                testTag = StandingOrdersTestTags.nextDate(order.standingOrderId),
            )
        }
        if (order.hasFinalPayment) {
            MetaLine(
                text = stringResource(
                    Res.string.feature_standing_orders_final_payment,
                    order.finalDateLabel,
                ),
                description = stringResource(
                    Res.string.feature_standing_orders_final_payment_accessibility,
                    order.finalDateLabel,
                ),
                testTag = StandingOrdersTestTags.finalDate(order.standingOrderId),
            )
        }
    }
}

@Composable
private fun StandingOrderDestinationLines(order: StandingOrderRowUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(META_LINE_GAP)) {
        if (order.sortCodeLabel.isNotBlank()) {
            MetaLine(
                text = order.sortCodeLabel,
                description = stringResource(
                    Res.string.feature_standing_orders_sort_code_accessibility,
                    order.sortCodeLabel,
                ),
                testTag = StandingOrdersTestTags.sortCode(order.standingOrderId),
                monospace = true,
            )
        }
        if (order.referenceLabel.isNotBlank()) {
            MetaLine(
                text = stringResource(
                    Res.string.feature_standing_orders_reference,
                    order.referenceLabel,
                ),
                description = stringResource(
                    Res.string.feature_standing_orders_reference_accessibility,
                    order.referenceLabel,
                ),
                testTag = StandingOrdersTestTags.reference(order.standingOrderId),
            )
        }
    }
}

/**
 * One small caption line on a card. [monospace] is set for the sort code and account number, where
 * the fixed-width digits are what make the grouping readable as an account identifier.
 */
@Composable
private fun MetaLine(
    text: String,
    description: String,
    testTag: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
) {
    val base = MaterialTheme.typography.bodySmall
    Text(
        text = text,
        style = if (monospace) {
            base.copy(fontFamily = FontFamily.Monospace, fontSize = SORT_CODE_SIZE)
        } else {
            base
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .testTag(testTag)
            .semantics { contentDescription = description },
    )
}
