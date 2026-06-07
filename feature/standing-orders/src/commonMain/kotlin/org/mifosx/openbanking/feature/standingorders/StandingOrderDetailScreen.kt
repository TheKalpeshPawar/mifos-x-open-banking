/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package org.mifosx.openbanking.feature.standingorders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifosx.openbanking.feature.standingorders.ui.ExecutionRow
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrderDetailContent
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrderDetailViewModel
import template.core.base.store.screen.ScreenState

/**
 * Full detail view for one standing order: status header, recipient, schedule, amount and
 * the observed execution history (booked TXN_TYPE=SO payments — each row drills into the
 * transaction detail screen). OBP has no pause/resume/cancel endpoint for standing orders,
 * so those actions surface a snackbar.
 */
@Composable
fun StandingOrderDetailScreen(
    bankId: String,
    accountId: String,
    standingOrderId: String,
    onExecutionClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StandingOrderDetailViewModel = koinViewModel {
        parametersOf(bankId, accountId, standingOrderId)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Standing Order", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ScreenState.Loading -> SodCentered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.Error,
                is ScreenState.NoNetwork,
                is ScreenState.Unauthenticated,
                -> SodStatusMessage(
                    title = "Standing Order Not Found",
                    message = "We could not load this standing order. It may have been " +
                        "cancelled or there may be a connection issue.",
                    actionLabel = "Retry",
                    onAction = viewModel::onRetry,
                )
                is ScreenState.Empty -> SodStatusMessage(
                    title = "No details available",
                    message = "No details are available for this standing order.",
                    actionLabel = "Go Back",
                    onAction = onBack,
                )
                is ScreenState.Content -> SodContent(
                    content = s.data,
                    onExecutionClick = onExecutionClick,
                    onNotify = notify,
                )
            }
        }
    }
}

@Composable
private fun SodContent(
    content: StandingOrderDetailContent,
    onExecutionClick: (String) -> Unit,
    onNotify: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        SodRecipientCard(content)
        Spacer(Modifier.height(12.dp))
        SodScheduleCard(content)
        Spacer(Modifier.height(12.dp))
        SodAmountCard(content)
        Spacer(Modifier.height(12.dp))
        SodHistoryCard(content, onExecutionClick)
        Spacer(Modifier.height(16.dp))
        SodActions(content, onNotify)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SodRecipientCard(content: StandingOrderDetailContent) {
    SodCard(
        title = "RECIPIENT",
        modifier = Modifier.testTag(StandingOrderDetailTestTags.RECIPIENT_CARD),
    ) {
        SodRow("Name", content.recipientName)
        if (content.recipientAccount.isNotBlank()) {
            SodDivider()
            SodRow("Account", content.recipientAccount, mono = true)
        }
    }
}

@Composable
private fun SodScheduleCard(content: StandingOrderDetailContent) {
    SodCard(
        title = "SCHEDULE",
        modifier = Modifier.testTag(StandingOrderDetailTestTags.SCHEDULE_CARD),
    ) {
        SodRow("Status", content.statusLabel, highlight = content.isActive)
        SodDivider()
        SodRow("Frequency", content.frequencyLabel)
        if (content.startedOn.isNotBlank()) {
            SodDivider()
            SodRow("First Payment", content.startedOn)
        }
        if (content.lastPayment.isNotBlank()) {
            SodDivider()
            SodRow("Last Payment", content.lastPayment)
        }
        if (content.nextPayment.isNotBlank()) {
            SodDivider()
            SodRow("Next Payment", content.nextPayment, highlight = content.isActive)
        }
        SodDivider()
        SodRow("Final Date", content.finalDate)
    }
}

@Composable
private fun SodAmountCard(content: StandingOrderDetailContent) {
    SodCard(
        title = "PAYMENT AMOUNT",
        modifier = Modifier.testTag(StandingOrderDetailTestTags.AMOUNT_CARD),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                content.amount,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    content.currency,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SodHistoryCard(content: StandingOrderDetailContent, onExecutionClick: (String) -> Unit) {
    SodCard(
        title = "RECENT EXECUTIONS",
        contentPadding = 4.dp,
        modifier = Modifier.testTag(StandingOrderDetailTestTags.HISTORY_CARD),
    ) {
        if (content.executions.isEmpty()) {
            Text(
                if (content.isCreated) "This order hasn't made a payment yet." else "No payments observed yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        } else {
            content.executions.forEachIndexed { index, row ->
                if (index > 0) SodDivider()
                SodExecutionRow(row, onClick = { onExecutionClick(row.transactionId) })
            }
        }
    }
}

@Composable
private fun SodExecutionRow(row: ExecutionRow, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag(StandingOrderDetailTestTags.executionRow(row.transactionId)),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            row.amountLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SodActions(content: StandingOrderDetailContent, onNotify: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        OutlinedButton(
            onClick = { onNotify("Pausing standing orders is coming soon") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag(StandingOrderDetailTestTags.PAUSE_BUTTON),
        ) {
            Icon(
                if (content.isActive) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(if (content.isActive) "Pause" else "Resume")
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(
            onClick = { onNotify("Cancelling standing orders is coming soon") },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f).testTag(StandingOrderDetailTestTags.CANCEL_BUTTON),
        ) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cancel")
        }
    }
}

@Composable
private fun SodCard(
    title: String,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = if (contentPadding < 16.dp) 12.dp else 0.dp,
                    top = if (contentPadding < 16.dp) 8.dp else 0.dp,
                    bottom = 8.dp,
                ),
            )
            content()
        }
    }
}

@Composable
private fun SodRow(label: String, value: String, mono: Boolean = false, highlight: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            value.ifBlank { "—" },
            style = if (mono) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else null,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Medium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SodDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SodStatusMessage(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    SodCentered {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun SodCentered(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { content() }
}
