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

package org.mifosx.openbanking.feature.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CloudOff
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifosx.openbanking.feature.transactions.ui.TransactionDetailContent
import org.mifosx.openbanking.feature.transactions.ui.TransactionDetailViewModel
import template.core.base.store.screen.ScreenState

/**
 * Full detail view for one booked transaction: amount hero with status badge, counterparty
 * card, field rows (date, reference with copy, type, accounts) and secondary actions.
 * Receipt download and disputes have no consumer OBP endpoint — they surface a snackbar.
 */
@Composable
fun TransactionDetailScreen(
    bankId: String,
    accountId: String,
    transactionId: String,
    onManageTags: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    requestId: String = "",
    viewModel: TransactionDetailViewModel = koinViewModel {
        parametersOf(bankId, accountId, transactionId, requestId)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details", fontWeight = FontWeight.SemiBold) },
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
                is ScreenState.Loading -> DetailCentered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.NoNetwork,
                is ScreenState.Unauthenticated,
                is ScreenState.Error,
                -> StatusMessage(
                    icon = { Icon(Icons.Outlined.CloudOff, contentDescription = null) },
                    title = "Transaction not found",
                    message = "We could not load this transaction. It may have been " +
                        "removed or there may be a connection issue.",
                    actionLabel = "Retry",
                    onAction = viewModel::onRetry,
                )
                is ScreenState.Empty -> StatusMessage(
                    icon = { Icon(Icons.Outlined.CloudOff, contentDescription = null) },
                    title = "Transaction details not available",
                    message = "No details are available for this transaction.",
                    actionLabel = "Go Back",
                    onAction = onBack,
                )
                is ScreenState.Content -> DetailContent(
                    content = s.data,
                    onManageTags = onManageTags,
                    onNotify = notify,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    content: TransactionDetailContent,
    onManageTags: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AmountHero(content)
        Spacer(Modifier.height(16.dp))
        CounterpartyCard(content)
        Spacer(Modifier.height(12.dp))
        DetailsCard(
            content = content,
            onCopyReference = {
                clipboard.setText(AnnotatedString(content.reference))
                onNotify("Reference copied")
            },
        )
        Spacer(Modifier.height(16.dp))
        if (!content.isPending) {
            OutlinedButton(
                onClick = { onNotify("Receipt download is coming soon") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag(TransactionDetailTestTags.DOWNLOAD_RECEIPT),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download Receipt")
            }
            Spacer(Modifier.height(8.dp))
            ManageTagsRow(onManageTags)
        }
        ReportIssueRow(onNotify)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AmountHero(content: TransactionDetailContent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 20.dp, vertical = 28.dp)
            .testTag(TransactionDetailTestTags.AMOUNT_HERO),
    ) {
        Text(
            content.amount,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = when {
                content.isPending -> MaterialTheme.colorScheme.onSurfaceVariant
                content.isDebit -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.testTag(TransactionDetailTestTags.AMOUNT_VALUE),
        )
        Spacer(Modifier.height(10.dp))
        val statusColor = if (content.isPending) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag(TransactionDetailTestTags.STATUS_BADGE),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    if (content.isPending) Icons.Filled.Schedule else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    content.statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
        }
    }
}

@Composable
private fun CounterpartyCard(content: TransactionDetailContent) {
    HouseCard(
        modifier = Modifier.padding(horizontal = 20.dp).testTag(TransactionDetailTestTags.MERCHANT_CARD),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            ) {
                Text(
                    content.counterpartyName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    content.counterpartyName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(TransactionDetailTestTags.MERCHANT_NAME),
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.testTag(TransactionDetailTestTags.CATEGORY_BADGE),
                ) {
                    Text(
                        content.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsCard(content: TransactionDetailContent, onCopyReference: () -> Unit) {
    HouseCard(
        modifier = Modifier.padding(horizontal = 20.dp).testTag(TransactionDetailTestTags.DETAILS_CARD),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow("Date & Time", content.dateTime)
            DetailDivider()
            ReferenceRow(content.reference, onCopyReference)
            DetailDivider()
            DetailRow("Transaction Type", content.typeLabel)
            DetailDivider()
            DetailRow("From Account", content.fromAccountLabel, secondary = content.fromAccountTail)
            DetailDivider()
            DetailRow(
                label = if (content.isDebit) "To" else "From",
                value = content.counterpartyName,
                secondary = content.toDetail.takeIf { it.isNotBlank() },
            )
            if (content.narrative.isNotBlank()) {
                DetailDivider()
                DetailRow("Note", content.narrative)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, secondary: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                value.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            )
            if (secondary != null) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReferenceRow(reference: String, onCopy: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Text(
            "Reference",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            reference.takeLast(REFERENCE_VISIBLE_CHARS),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(32.dp).testTag(TransactionDetailTestTags.COPY_REFERENCE),
        ) {
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Copy reference number",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ManageTagsRow(onManageTags: () -> Unit) {
    HouseCard(
        modifier = Modifier.padding(horizontal = 20.dp).testTag(TransactionDetailTestTags.MANAGE_TAGS),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clickable(onClick = onManageTags)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(
                Icons.Filled.Label,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Tags & Notes",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Add tags or notes to this transaction",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportIssueRow(onNotify: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(
            onClick = { onNotify("Disputes are coming soon") },
            modifier = Modifier.testTag(TransactionDetailTestTags.REPORT_ISSUE),
        ) {
            Icon(
                Icons.Filled.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Report an Issue",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun HouseCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) { content() }
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StatusMessage(
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    DetailCentered {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            icon()
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
private fun DetailCentered(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { content() }
}

private const val REFERENCE_VISIBLE_CHARS = 16
