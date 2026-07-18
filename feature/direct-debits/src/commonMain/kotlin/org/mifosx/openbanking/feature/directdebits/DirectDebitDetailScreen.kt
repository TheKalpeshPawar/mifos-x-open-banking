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

package org.mifosx.openbanking.feature.directdebits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitDetailContent
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitDetailViewModel
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitPaymentRow
import template.core.base.store.screen.ScreenState

/**
 * Direct Debit Detail — one merchant mandate derived from `TXN_TYPE=DD` history. Shows the
 * merchant, amount and cadence, the mandate metadata (next payment, linked account, reference,
 * start date) and the recent collected payments, with a cancel action recorded locally (OBP
 * has no cancel endpoint). There is no edit affordance: direct debits are merchant-initiated,
 * so a consumer can only view and cancel. [onViewAllPayments] opens the account's full history.
 */
@Composable
fun DirectDebitDetailScreen(
    onBack: () -> Unit,
    onViewAllPayments: () -> Unit,
    modifier: Modifier = Modifier,
    bankId: String = "",
    accountId: String = "",
    mandateId: String = "",
    viewModel: DirectDebitDetailViewModel = koinViewModel { parametersOf(bankId, accountId, mandateId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Direct Debit",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(DirectDebitDetailTestTags.TITLE),
                    )
                },
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
                is ScreenState.Loading -> DddCentered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.Empty -> DddEmptyState(onBack = onBack)
                is ScreenState.Error,
                is ScreenState.NoNetwork,
                is ScreenState.Unauthenticated,
                -> DddErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Content -> DddContent(
                    content = s.data,
                    onViewAllPayments = onViewAllPayments,
                    onCancelRequested = viewModel::onCancelRequested,
                    onCancelConfirmed = viewModel::onCancelConfirmed,
                    onCancelDismissed = viewModel::onCancelDismissed,
                )
            }
        }
    }
}

@Composable
private fun DddContent(
    content: DirectDebitDetailContent,
    onViewAllPayments: () -> Unit,
    onCancelRequested: () -> Unit,
    onCancelConfirmed: () -> Unit,
    onCancelDismissed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        DddHeader(content)
        Spacer(Modifier.height(20.dp))
        DddDetailsCard(content)
        Spacer(Modifier.height(12.dp))
        DddHistoryCard(content, onViewAllPayments)
        if (content.isActive) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onCancelRequested,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().testTag(DirectDebitDetailTestTags.CANCEL_CTA),
            ) {
                Text("Cancel Mandate")
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (content.cancelDialogVisible) {
        DddCancelDialog(
            merchant = content.merchantName,
            reference = content.mandateReference,
            onConfirm = onCancelConfirmed,
            onDismiss = onCancelDismissed,
        )
    }
}

@Composable
private fun DddHeader(content: DirectDebitDetailContent) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            content.merchantName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(DirectDebitDetailTestTags.MERCHANT_NAME),
        )
        Spacer(Modifier.height(8.dp))
        DddStatusBadge(label = content.statusLabel, active = content.isActive)
        Spacer(Modifier.height(12.dp))
        Text(
            content.amountLabel,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(DirectDebitDetailTestTags.AMOUNT),
        )
        Text(
            content.frequencyLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DddDetailsCard(content: DirectDebitDetailContent) {
    DddCard(
        title = "Mandate Details",
        modifier = Modifier.testTag(DirectDebitDetailTestTags.DETAILS_CARD),
    ) {
        DddRow("Next Payment", content.nextPaymentLabel)
        DddDivider()
        DddRow("Account", content.linkedAccountName, secondary = content.linkedAccountMasked)
        DddDivider()
        DddRow("Mandate Ref", content.mandateReference, mono = true)
        DddDivider()
        DddRow("Start Date", content.startDateLabel)
    }
}

@Composable
private fun DddHistoryCard(content: DirectDebitDetailContent, onViewAllPayments: () -> Unit) {
    DddCard(
        title = "Recent Payments",
        modifier = Modifier.testTag(DirectDebitDetailTestTags.HISTORY_CARD),
        action = {
            TextButton(
                onClick = onViewAllPayments,
                modifier = Modifier.testTag(DirectDebitDetailTestTags.VIEW_ALL),
            ) {
                Text("View all", style = MaterialTheme.typography.labelMedium)
            }
        },
    ) {
        if (content.recentPayments.isEmpty()) {
            Text(
                "No payments collected yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            content.recentPayments.forEachIndexed { index, payment ->
                if (index > 0) DddDivider()
                DddPaymentRow(payment)
            }
        }
    }
}

@Composable
private fun DddPaymentRow(payment: DirectDebitPaymentRow) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Text(
            payment.dateLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            payment.amountLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DddCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                action?.invoke()
            }
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun DddRow(label: String, value: String, secondary: String? = null, mono: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (mono) FontFamily.Monospace else null,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (secondary != null) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DddDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DddStatusBadge(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        modifier = Modifier.testTag(DirectDebitDetailTestTags.STATUS_BADGE),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (active) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun DddCancelDialog(
    merchant: String,
    reference: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Direct Debit?", fontWeight = FontWeight.Bold) },
        text = {
            Text("$merchant ($reference) will stop collecting payments. This cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag(DirectDebitDetailTestTags.CANCEL_CONFIRM),
            ) {
                Text("Yes, Cancel Mandate")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag(DirectDebitDetailTestTags.CANCEL_DISMISS),
            ) {
                Text("Keep Mandate")
            }
        },
        modifier = Modifier.testTag(DirectDebitDetailTestTags.CANCEL_DIALOG),
    )
}

@Composable
private fun DddEmptyState(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.EventBusy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Mandate not available",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This direct debit could not be found. It may have already been cancelled or expired.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag(DirectDebitDetailTestTags.EMPTY_BACK),
        ) {
            Text("Back to Direct Debits")
        }
    }
}

@Composable
private fun DddErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Unable to load mandate",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Check your connection and try again",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onRetry,
            modifier = Modifier.testTag(DirectDebitDetailTestTags.RETRY),
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun DddCentered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
