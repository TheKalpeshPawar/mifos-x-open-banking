/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.businessinsights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.businessinsights.ui.BizContent

/** Money In / Money Out / Net for the selected period — the business framing. */
@Composable
internal fun BizCashFlowCard(content: BizContent) {
    Column {
        BizCard(modifier = Modifier.testTag("biz_cash_flow_card")) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                BizFlowStat("Money In", content.moneyIn, content.currency, Modifier.weight(1f))
                BizFlowStat("Money Out", -content.moneyOut, content.currency, Modifier.weight(1f))
                BizFlowStat("Net", content.net, content.currency, Modifier.weight(1f), emphasize = true)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BizFlowStat(
    label: String,
    value: Double,
    currency: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            bizFormatMoney(value, currency),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = when {
                emphasize && value >= 0 -> MaterialTheme.colorScheme.primary
                emphasize -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/** Expense donut + per-category rows over the business taxonomy. */
@Composable
internal fun BizCategorySection(content: BizContent) {
    if (content.categories.isEmpty()) return
    Column {
        BizSectionTitle("Expenses by Category")
        BizCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                BizDonutChart(content, modifier = Modifier.size(140.dp))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    content.categories.forEachIndexed { index, category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("biz_category_${category.id}"),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(bizDonutColor(index), CircleShape),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                category.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                bizFormatMoney(-category.amount, content.currency),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Top counterparties by period outflow. */
@Composable
internal fun BizMerchantsSection(content: BizContent) {
    if (content.topMerchants.isEmpty()) return
    Column {
        BizSectionTitle("Top Counterparties")
        BizCard {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content.topMerchants.forEach { merchant ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                merchant.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (merchant.count == 1) "1 payment" else "${merchant.count} payments",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            bizFormatMoney(-merchant.amount, content.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BizDonutChart(content: BizContent, modifier: Modifier = Modifier) {
    val colors = content.categories.indices.map { bizDonutColor(it) }
    Box(contentAlignment = Alignment.Center, modifier = modifier.testTag("biz_donut_chart")) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val stroke = Stroke(width = 28f)
            val total = content.categories.sumOf { it.amount }.takeIf { it > 0 } ?: return@Canvas
            var startAngle = -90f
            content.categories.forEachIndexed { index, category ->
                val sweep = (category.amount / total * 360f).toFloat()
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width - stroke.width, size.height - stroke.width),
                    topLeft = Offset(stroke.width / 2, stroke.width / 2),
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                bizFormatMoney(content.moneyOut, content.currency),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "out",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BizSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun BizCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) { content() }
}

@Composable
private fun bizDonutColor(index: Int): Color {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant,
    )
    return palette[index % palette.size]
}

/** "1029.8", "EUR" → "€1029.80" (negative values keep the minus sign). */
internal fun bizFormatMoney(value: Double, currency: String): String {
    val abs = if (value < 0) -value else value
    val cents = kotlin.math.round(abs * 100).toLong()
    val sign = if (value < 0) "-" else ""
    return "$sign${bizCurrencySymbol(currency)}${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

internal fun bizCurrencySymbol(currency: String): String = when (currency.uppercase()) {
    "GBP" -> "£"
    "EUR" -> "€"
    "USD" -> "$"
    else -> if (currency.isBlank()) "" else "$currency "
}
