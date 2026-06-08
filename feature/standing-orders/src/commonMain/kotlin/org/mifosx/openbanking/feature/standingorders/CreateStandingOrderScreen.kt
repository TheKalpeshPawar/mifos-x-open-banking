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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.ui.picker.AccountPickerBottomSheet
import org.mifosx.openbanking.feature.standingorders.ui.CreateStandingOrderViewModel
import org.mifosx.openbanking.feature.standingorders.ui.STANDING_ORDER_FREQUENCIES
import org.mifosx.openbanking.feature.standingorders.ui.recurrenceHint
import template.core.base.platform.toast.rememberToastLauncher
import kotlin.time.Instant

/**
 * New Standing Order screen. Real-world scheduling model: payee + amount + frequency +
 * first payment date (the recurrence anchor) + optional end date. The recurrence hint
 * under the date spells out what the chosen date implies.
 */
@Composable
fun CreateStandingOrderScreen(
    accountId: String,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateStandingOrderViewModel = koinViewModel(parameters = { parametersOf(accountId) }),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val toast = rememberToastLauncher()

    LaunchedEffect(form.created) {
        if (form.created) {
            // A toast outlives this screen, so the confirmation stays visible after the pop.
            toast("Standing order created")
            onCreated()
        }
    }
    LaunchedEffect(form.error) {
        form.error?.let { toast(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New Standing Order", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        if (form.loadingPayees) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (form.sourceAccountName.isNotBlank()) {
                SourceAccountField(
                    accounts = form.accounts,
                    selectedAccountId = form.selectedAccountId,
                    label = form.sourceAccountName,
                    onAccountSelected = viewModel::onSourceAccountSelected,
                )
                Spacer(Modifier.height(12.dp))
            }
            PayeeField(
                payees = form.payees.map { it.counterpartyId to it.name },
                selectedPayeeId = form.selectedPayeeId,
                onPayeeSelected = viewModel::onPayeeSelected,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.amount,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("Amount") },
                suffix = { Text(form.currency) },
                supportingText = { Text("Amount taken on each payment date") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(StandingOrdersTestTags.CREATE_AMOUNT),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Repeats",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                STANDING_ORDER_FREQUENCIES.forEach { frequency ->
                    FilterChip(
                        selected = form.frequency == frequency,
                        onClick = { viewModel.onFrequencySelected(frequency) },
                        label = { Text(frequencyChipLabel(frequency)) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            DateField(
                label = "First payment date",
                helper = "First payment runs on this date",
                date = form.startDate,
                minDate = null,
                clearable = false,
                tag = StandingOrdersTestTags.CREATE_START_DATE,
                onDateSelected = viewModel::onStartDateSelected,
                onCleared = {},
            )
            val hint = recurrenceHint(form.frequency, form.startDate)
            if (hint.isNotBlank()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            DateField(
                label = "End date (optional)",
                helper = "Leave empty to pay until cancelled",
                date = form.endDate,
                minDate = form.startDate,
                clearable = true,
                tag = StandingOrdersTestTags.CREATE_END_DATE,
                onDateSelected = { viewModel.onEndDateSelected(it) },
                onCleared = { viewModel.onEndDateSelected(null) },
            )
            form.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = viewModel::onSubmit,
                enabled = !form.submitting,
                modifier = Modifier.fillMaxWidth().testTag(StandingOrdersTestTags.CREATE_SUBMIT),
            ) {
                Text(if (form.submitting) "Creating…" else "Create standing order")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Source-account field. Tapping it opens the shared account picker sheet; switching
 * accounts reloads the payee list (a standing order pays a counterparty of its source
 * account, so payees always track the selected account).
 */
@Composable
private fun SourceAccountField(
    accounts: List<Account>,
    selectedAccountId: String,
    label: String,
    onAccountSelected: (Account) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("From account") },
            trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag(StandingOrdersTestTags.CREATE_SOURCE_ACCOUNT),
        )
        Box(modifier = Modifier.matchParentSize().clickable { showPicker = true })
    }
    if (showPicker) {
        AccountPickerBottomSheet(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onAccountSelected = { account ->
                showPicker = false
                onAccountSelected(account)
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun PayeeField(
    payees: List<Pair<String, String>>,
    selectedPayeeId: String,
    onPayeeSelected: (String) -> Unit,
) {
    if (payees.isEmpty()) {
        Text(
            text = "Add a beneficiary first — standing orders pay an existing payee.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        return
    }
    var expanded by remember { mutableStateOf(false) }
    val selected = payees.firstOrNull { it.first == selectedPayeeId }
    Box {
        OutlinedTextField(
            value = selected?.second ?: "Select payee",
            onValueChange = {},
            readOnly = true,
            label = { Text("Pay to") },
            trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag(StandingOrdersTestTags.CREATE_PAYEE),
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            payees.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onPayeeSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    helper: String,
    date: LocalDate?,
    minDate: LocalDate?,
    clearable: Boolean,
    tag: String,
    onDateSelected: (LocalDate) -> Unit,
    onCleared: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = date?.let { formatPickedDate(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            supportingText = { Text(helper) },
            trailingIcon = {
                if (clearable && date != null) {
                    IconButton(onClick = onCleared) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear $label")
                    }
                } else {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (clearable && date != null) 48.dp else 0.dp)
                .clickable { showPicker = true },
        )
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: minDate)?.toEpochMillis(),
            selectableDates = futureSelectableDates(minDate),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDateSelected(it.toLocalDate()) }
                        showPicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Only dates strictly after today (and after [minDate], when given) are selectable. */
private fun futureSelectableDates(minDate: LocalDate?): SelectableDates = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val candidate = utcTimeMillis.toLocalDate()
        if (minDate != null && candidate <= minDate) return false
        return true
    }
}

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

private fun formatPickedDate(date: LocalDate): String {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )
    return "${date.day} ${months[date.month.number - 1]} ${date.year}"
}

private fun frequencyChipLabel(frequency: String): String = when (frequency) {
    "DAILY" -> "Daily"
    "WEEKLY" -> "Weekly"
    "BI-WEEKLY" -> "Fortnightly"
    "YEARLY" -> "Yearly"
    else -> "Monthly"
}
