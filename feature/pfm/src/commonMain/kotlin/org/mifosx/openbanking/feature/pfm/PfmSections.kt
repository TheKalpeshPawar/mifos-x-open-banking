/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.pfm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.pfm.ui.BudgetRow
import org.mifosx.openbanking.feature.pfm.ui.PfmContent
import org.mifosx.openbanking.feature.pfm.ui.formatMoney

@Composable
internal fun PfmSummaryCard(content: PfmContent) {
    PfmCard(modifier = Modifier.testTag(PfmDashboardTestTags.SUMMARY_CARD)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryColumn(
                    label = "Total Spent",
                    value = formatMoney(-content.summary.spent, content.currency),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                SummaryColumn(
                    label = "Received",
                    value = formatMoney(content.summary.received, content.currency),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                SummaryColumn(
                    label = "Net",
                    value = (if (content.summary.net >= 0) "+" else "") +
                        formatMoney(content.summary.net, content.currency),
                    color = if (content.summary.net >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryColumn(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
internal fun PfmBudgetOverview(content: PfmContent, onSaveBudget: (String, String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val overall = content.overallBudget
    if (overall != null) {
        PfmCard(
            modifier = Modifier
                .clickable { editing = true }
                .testTag(PfmDashboardTestTags.OVERALL_BUDGET_CARD),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Monthly Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${overall.percent}% used",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${formatMoney(overall.spent, content.currency)} spent",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${formatMoney(overall.remaining, content.currency)} left",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                PfmProgressBar(percent = overall.percent)
                Spacer(Modifier.height(6.dp))
                Text(
                    "of ${formatMoney(overall.limit, content.currency)} monthly budget — tap to change",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    } else {
        PfmCard(
            modifier = Modifier
                .clickable { editing = true }
                .testTag(PfmDashboardTestTags.NO_BUDGET_BANNER),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "No budget set",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Set a monthly budget to track how much you spend against your target. Tap to set one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    if (editing) {
        BudgetEditDialog(
            title = "Monthly budget",
            currentLimit = overall?.limit,
            onDismiss = { editing = false },
            onSave = { amount ->
                editing = false
                onSaveBudget("overall", amount)
            },
        )
    }
}

@Composable
internal fun PfmCategorySection(content: PfmContent) {
    if (content.categories.isEmpty()) return
    Column {
        PfmSectionTitle("Spending by Category")
        PfmCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                PfmDonutChart(content, modifier = Modifier.size(140.dp))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    content.categories.forEachIndexed { index, category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag(PfmDashboardTestTags.categoryRow(category.id)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(donutColor(index), CircleShape),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                category.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                formatMoney(category.amount, content.currency),
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

@Composable
private fun PfmDonutChart(content: PfmContent, modifier: Modifier = Modifier) {
    val colors = content.categories.indices.map { donutColor(it) }
    Box(contentAlignment = Alignment.Center, modifier = modifier.testTag(PfmDashboardTestTags.DONUT_CHART)) {
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
                    topLeft = androidx.compose.ui.geometry.Offset(stroke.width / 2, stroke.width / 2),
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatMoney(content.summary.spent, content.currency),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun donutColor(index: Int): Color {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.surfaceTint,
        MaterialTheme.colorScheme.inversePrimary,
        MaterialTheme.colorScheme.errorContainer,
    )
    return palette[index % palette.size]
}

@Composable
internal fun PfmBudgetRowsSection(content: PfmContent, onSaveBudget: (String, String) -> Unit) {
    var editing by remember { mutableStateOf<BudgetRow?>(null) }
    Column {
        PfmSectionTitle("Budget Progress")
        PfmCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                content.budgetRows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = row }
                            .padding(vertical = 10.dp)
                            .testTag(PfmDashboardTestTags.budgetRow(row.categoryId)),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                row.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (row.limit != null) {
                                    "${formatMoney(row.spent, content.currency)} / " +
                                        formatMoney(row.limit, content.currency)
                                } else {
                                    "${formatMoney(row.spent, content.currency)} · Set budget"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (row.percent != null && row.percent >= NEAR_LIMIT_PERCENT) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        if (row.percent != null) {
                            Spacer(Modifier.height(6.dp))
                            PfmProgressBar(percent = row.percent)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        editing?.let { row ->
            BudgetEditDialog(
                title = "${row.label} budget",
                currentLimit = row.limit,
                onDismiss = { editing = null },
                onSave = { amount ->
                    editing = null
                    onSaveBudget(row.categoryId, amount)
                },
            )
        }
    }
}

@Composable
private fun PfmProgressBar(percent: Int) {
    val fraction = (percent / 100f).coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { fraction },
        color = when {
            percent >= NEAR_LIMIT_PERCENT -> MaterialTheme.colorScheme.error
            percent >= WARN_PERCENT -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth().height(8.dp),
    )
}

@Composable
private fun BudgetEditDialog(
    title: String,
    currentLimit: Double?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember {
        mutableStateOf(currentLimit?.let { if (it % 1.0 == 0.0) "${it.toLong()}" else "$it" }.orEmpty())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Monthly limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = { Button(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
internal fun PfmMerchantsSection(content: PfmContent, onMerchantClick: () -> Unit) {
    if (content.topMerchants.isEmpty()) return
    Column {
        PfmSectionTitle("Top Merchants")
        PfmCard(modifier = Modifier.testTag(PfmDashboardTestTags.MERCHANTS_CARD)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                content.topMerchants.forEachIndexed { index, merchant ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onMerchantClick)
                            .padding(vertical = 10.dp)
                            .testTag(PfmDashboardTestTags.merchantRow(merchant.name)),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        ) {
                            Text(
                                merchant.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                merchant.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (merchant.count == 1) "1 transaction" else "${merchant.count} transactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            formatMoney(-merchant.amount, content.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun PfmSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp),
    )
}

@Composable
internal fun PfmCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) { content() }
}

private const val NEAR_LIMIT_PERCENT = 90
private const val WARN_PERCENT = 70
