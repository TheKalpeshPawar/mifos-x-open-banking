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

package org.mifosx.openbanking.feature.sendmoney

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.feature.sendmoney.ui.PaymentDraft
import org.mifosx.openbanking.feature.sendmoney.ui.PaymentType
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyContent
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyViewModel
import template.core.base.store.screen.ScreenState

@Composable
fun SendMoneyScreen(
    onContinue: (PaymentDraft) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    preselectedCounterpartyId: String = "",
    accountId: String = "",
    viewModel: SendMoneyViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(accountId) {
        if (accountId.isNotBlank()) {
            viewModel.setAccount(accountId)
        }
    }

    LaunchedEffect(preselectedCounterpartyId) {
        if (preselectedCounterpartyId.isNotBlank()) {
            viewModel.onBeneficiarySelected(preselectedCounterpartyId)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Send Money", fontWeight = FontWeight.SemiBold) },
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
                is ScreenState.Loading -> Centered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.Empty -> Centered {
                    Text(
                        "No accounts available to send from.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                is ScreenState.Error -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.NoNetwork -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Unauthenticated -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Content -> Form(
                    content = s.data,
                    viewModel = viewModel,
                    onContinue = onContinue,
                    locked = preselectedCounterpartyId.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun Form(
    content: SendMoneyContent,
    viewModel: SendMoneyViewModel,
    onContinue: (PaymentDraft) -> Unit,
    locked: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AccountSelector(selected = content.selectedAccount)

        OutlinedTextField(
            value = content.amount,
            onValueChange = viewModel::onAmountChanged,
            label = { Text("Amount") },
            prefix = { Text(currencySymbol(content.currency)) },
            placeholder = { Text("0.00") },
            singleLine = true,
            isError = content.amountError != null,
            supportingText = content.amountError?.let { { Text(it) } },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.AMOUNT_INPUT),
        )

        Text(
            "To",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val selected = content.selectedBeneficiary
        if (locked && selected != null) {
            // Recipient already chosen on the hub — show it locked, no picker.
            BeneficiaryRow(beneficiary = selected, selected = true, onClick = {})
        } else {
            OutlinedTextField(
                value = content.query,
                onValueChange = viewModel::onBeneficiaryQueryChanged,
                placeholder = { Text("Search beneficiary...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                isError = content.beneficiaryError != null,
                supportingText = content.beneficiaryError?.let { { Text(it) } },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.BENEFICIARY_SEARCH),
            )
            content.beneficiaries.forEach { b ->
                BeneficiaryRow(
                    beneficiary = b,
                    selected = content.selectedBeneficiary?.counterpartyId == b.counterpartyId,
                    onClick = { viewModel.onBeneficiarySelected(b.counterpartyId) },
                )
            }
        }

        OutlinedTextField(
            value = content.reference,
            onValueChange = viewModel::onReferenceChanged,
            label = { Text("Reference") },
            placeholder = { Text("Payment for invoice #1234") },
            singleLine = true,
            supportingText = { Text("Max 35 characters") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.REFERENCE_INPUT),
        )

        Text(
            "Payment Type",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentType.entries.forEach { type ->
                FilterChip(
                    selected = content.paymentType == type,
                    onClick = { viewModel.onPaymentTypeChanged(type) },
                    label = { Text(type.label()) },
                    modifier = Modifier.testTag(SendMoneyTestTags.paymentType(type.name)),
                )
            }
        }

        FeeBanner(paymentType = content.paymentType)

        content.formError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { viewModel.onContinue(onContinue) },
            enabled = content.continueEnabled,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(SendMoneyTestTags.CONTINUE_BUTTON),
        ) {
            Text(if (content.submitting) "Checking..." else "Continue")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AccountSelector(selected: Account) {
    // Read-only: payments send from the account that owns the beneficiaries (no switching —
    // a counterparty is only valid on its owning account).
    OutlinedTextField(
        value = accountLabel(selected),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text("From Account") },
        leadingIcon = { Icon(Icons.Filled.AccountBalance, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.FROM_ACCOUNT),
    )
}

@Composable
private fun BeneficiaryRow(beneficiary: Counterparty, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.beneficiaryChip(beneficiary.counterpartyId)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                beneficiary.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val sub = beneficiary.otherAccountRoutingAddress
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FeeBanner(paymentType: PaymentType) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().testTag(SendMoneyTestTags.FEE_BANNER),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Estimated fee: ${feeText(paymentType)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
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
            "Could not load payment form",
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
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

private fun PaymentType.label(): String = when (this) {
    PaymentType.SEPA -> "SEPA"
    PaymentType.DOMESTIC -> "Domestic"
    PaymentType.INTERNATIONAL -> "International"
}

private fun feeText(type: PaymentType): String = when (type) {
    PaymentType.SEPA -> "Free (SEPA)"
    PaymentType.DOMESTIC -> "Free (Domestic)"
    PaymentType.INTERNATIONAL -> "Varies (International)"
}

private fun accountLabel(account: Account): String {
    val label = account.label.ifBlank { account.accountIdOrId }
    val bal = account.balance
    return if (bal.currency.isBlank()) label else "$label — ${currencySymbol(bal.currency)}${bal.amount}"
}

internal fun currencySymbol(currency: String): String = when (currency.uppercase()) {
    "GBP" -> "£"
    "EUR" -> "€"
    "USD" -> "$"
    else -> if (currency.isBlank()) "" else "$currency "
}
