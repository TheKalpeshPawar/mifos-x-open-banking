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

package org.mifosx.openbanking.feature.fxrates

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.mifosx.openbanking.feature.fxrates.ui.FxPairRow
import org.mifosx.openbanking.feature.fxrates.ui.FxRatesContent
import org.mifosx.openbanking.feature.fxrates.ui.FxRatesViewModel
import template.core.base.store.screen.ScreenState

/**
 * Exchange Rates — live currency converter (amount, from/to currency pickers, swap) over
 * the OBP FX endpoint, plus tappable popular pairs that populate the converter. The CTA
 * hands off to Send Money.
 */
@Composable
fun FxRatesScreen(
    onSendMoney: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FxRatesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Exchange Rates",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(FxRatesTestTags.TITLE),
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
                is ScreenState.Loading -> FxCentered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.Empty,
                is ScreenState.Error,
                is ScreenState.NoNetwork,
                is ScreenState.Unauthenticated,
                -> FxErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Content -> FxRatesContent(
                    content = s.data,
                    viewModel = viewModel,
                    onSendMoney = onSendMoney,
                )
            }
        }
    }
}

@Composable
private fun FxRatesContent(
    content: FxRatesContent,
    viewModel: FxRatesViewModel,
    onSendMoney: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (content.lastUpdatedLabel.isNotBlank()) {
            Text(
                content.lastUpdatedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .testTag(FxRatesTestTags.LAST_UPDATED),
            )
        }
        FxConverterCard(content, viewModel, onSendMoney)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            "Popular Pairs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(FxRatesTestTags.POPULAR_PAIRS_HEADER),
        )
        content.pairs.forEach { pair ->
            FxPairRowCard(pair) { viewModel.onPairSelected(pair.fromCurrency, pair.toCurrency) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FxConverterCard(
    content: FxRatesContent,
    viewModel: FxRatesViewModel,
    onSendMoney: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag(FxRatesTestTags.CONVERTER_CARD),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = content.amountInput,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("I want to send") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FxRatesTestTags.SEND_AMOUNT_INPUT),
            )
            Spacer(Modifier.height(12.dp))
            CurrencySelector(
                label = "From",
                selected = content.fromCurrency,
                currencies = content.currencies,
                onSelected = viewModel::onFromCurrencySelected,
                testTag = FxRatesTestTags.FROM_CURRENCY_SELECT,
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                FilledIconButton(
                    onClick = viewModel::onSwapCurrencies,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.testTag(FxRatesTestTags.SWAP_CURRENCIES),
                ) {
                    Icon(
                        Icons.Filled.SwapVert,
                        contentDescription = "Swap currencies — exchange from and to currencies",
                    )
                }
            }
            CurrencySelector(
                label = "To",
                selected = content.toCurrency,
                currencies = content.currencies,
                onSelected = viewModel::onToCurrencySelected,
                testTag = FxRatesTestTags.TO_CURRENCY_SELECT,
            )
            Spacer(Modifier.height(16.dp))
            if (content.rateAvailable) {
                Text(
                    content.convertedLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(FxRatesTestTags.CONVERTED_RESULT),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    content.rateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(FxRatesTestTags.RATE_INFO),
                )
            } else {
                Text(
                    "No exchange rate available for ${content.fromCurrency} / ${content.toCurrency}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(FxRatesTestTags.CONVERTED_RESULT),
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSendMoney,
                enabled = content.rateAvailable,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FxRatesTestTags.SEND_MONEY_CTA),
            ) {
                Text("Send this amount", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun CurrencySelector(
    label: String,
    selected: String,
    currencies: List<String>,
    onSelected: (String) -> Unit,
    testTag: String,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().testTag(testTag),
        ) {
            Text(
                "$label:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                selected,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Change $label currency, currently $selected",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            currencies.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        expanded = false
                        onSelected(code)
                    },
                )
            }
        }
    }
}

@Composable
private fun FxPairRowCard(pair: FxPairRow, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .testTag(FxRatesTestTags.pairRow(pair.fromCurrency, pair.toCurrency)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                "${pair.fromCurrency} / ${pair.toCurrency}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                pair.rateLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FxCentered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun FxErrorState(onRetry: () -> Unit) {
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
            "Unable to load exchange rates. Check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}
