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

package org.mifosx.openbanking.feature.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.CloudOff
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.card.HeroGradientCard
import org.mifosx.openbanking.feature.accounts.ui.AccountDetailContent
import org.mifosx.openbanking.feature.accounts.ui.AccountDetailViewModel
import org.mifosx.openbanking.feature.accounts.ui.AttributeRow
import template.core.base.store.screen.ScreenState

/**
 * Account-detail screen — a read-only account summary. Header (label, balance, currency),
 * an Account & routing card (holder, number, IBAN, bank — each copyable), and a Plan & fees
 * card (product + account attributes). No transactions, no actions. Pushed destination: owns
 * its top bar; the bottom nav is supplied by the navbar host.
 */
@Composable
fun AccountDetailScreen(
    bankId: String,
    accountId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onViewTransactions: () -> Unit = {},
    onViewDirectDebits: () -> Unit = {},
    viewModel: AccountDetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(bankId, accountId) { viewModel.load(bankId, accountId) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val onCopy: (String, String) -> Unit = { value, what ->
        clipboard.setText(AnnotatedString(value))
        scope.launch { snackbarHostState.showSnackbar("$what copied") }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Account Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ScreenState.Loading -> Centered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.Error -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.NoNetwork -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Unauthenticated -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Empty -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Content -> Loaded(
                    content = s.data,
                    onCopy = onCopy,
                    onViewTransactions = onViewTransactions,
                    onViewDirectDebits = onViewDirectDebits,
                )
            }
        }
    }
}

@Composable
private fun Loaded(
    content: AccountDetailContent,
    onCopy: (value: String, what: String) -> Unit,
    onViewTransactions: () -> Unit,
    onViewDirectDebits: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeaderCard(content)

        GroupLabel("ACTIVITY", topPadding = 20)
        ActivityRow(
            title = "Transaction history",
            subtitle = "Booked and pending payments",
            onClick = onViewTransactions,
            bottomPadding = 8.dp,
        )
        ActivityRow(
            title = "Direct debits",
            subtitle = "Mandates collecting from this account",
            onClick = onViewDirectDebits,
            bottomPadding = 16.dp,
        )

        GroupLabel("ACCOUNT & ROUTING", topPadding = 8)
        InfoCard {
            InfoRow(label = "Account Holder", value = content.holder)
            if (content.accountNumber.isNotBlank()) {
                RowDivider()
                InfoRow(
                    label = "Account Number",
                    value = content.accountNumber,
                    copyValue = content.accountNumber,
                    copyWhat = "Account number",
                    onCopy = onCopy,
                )
            }
            if (content.ibanRaw.isNotBlank()) {
                RowDivider()
                InfoRow(
                    label = "IBAN",
                    value = content.ibanDisplay,
                    copyValue = content.ibanRaw,
                    copyWhat = "IBAN",
                    onCopy = onCopy,
                )
            }
            if (content.bankName.isNotBlank()) {
                RowDivider()
                InfoRow(label = "Bank", value = content.bankName)
            }
        }

        if (content.hasPlanSection) {
            GroupLabel("PLAN & FEES", topPadding = 8)
            InfoCard {
                if (content.productLabel.isNotBlank()) {
                    InfoRow(label = "Product", value = content.productLabel)
                }
                content.attributes.forEachIndexed { index, attr ->
                    if (index > 0 || content.productLabel.isNotBlank()) RowDivider()
                    AttributeInfoRow(attr)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun HeaderCard(content: AccountDetailContent) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    HeroGradientCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp)) {
            Text(
                text = content.accountLabel,
                style = MaterialTheme.typography.titleSmall,
                color = onPrimary.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = content.balanceDisplay,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = onPrimary,
            )
            if (content.currency.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    contentColor = onPrimary,
                ) {
                    Text(
                        text = content.currency,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    bottomPadding: Dp,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = bottomPadding),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GroupLabel(text: String, topPadding: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = topPadding.dp, bottom = 8.dp),
    )
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    copyValue: String? = null,
    copyWhat: String = "",
    onCopy: ((String, String) -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (copyValue != null && onCopy != null) {
            IconButton(
                onClick = { onCopy(copyValue, copyWhat) },
                modifier = Modifier.testTag(AccountDetailTestTags.copyButton(copyWhat)),
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy $copyWhat",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun AttributeInfoRow(attr: AttributeRow) = InfoRow(label = attr.label, value = attr.value)

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Couldn't load account details",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text("Try again") }
    }
}

/** UI test tags for the account-detail screen. */
object AccountDetailTestTags {
    const val SCREEN = "account_detail_screen"
    fun copyButton(what: String) = "account_detail_copy_${what.lowercase().replace(' ', '_')}"
}
