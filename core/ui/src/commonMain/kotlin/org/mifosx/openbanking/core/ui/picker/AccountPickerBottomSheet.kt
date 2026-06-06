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

package org.mifosx.openbanking.core.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.core.model.obp.Account

/**
 * Reusable account picker bottom sheet. Lists [accounts] with a divider between rows;
 * the row matching [selectedAccountId] carries a check. Selecting a row reports the
 * account and dismisses; swiping down / tapping outside calls [onDismiss].
 */
@Composable
fun AccountPickerBottomSheet(
    accounts: List<Account>,
    selectedAccountId: String,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Choose account",
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.testTag(AccountPickerTestTags.SHEET),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        // Scrollable so long account lists never clip mid-row at the sheet fold (a clipped
        // row reads as a stray divider line below the last visible account).
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(accounts, key = { _, account -> account.accountIdOrId }) { index, account ->
                Column {
                    AccountRow(
                        account = account,
                        selected = account.accountIdOrId == selectedAccountId,
                        onClick = { onAccountSelected(account) },
                    )
                    // Divider BETWEEN rows only — never under the last account.
                    if (index < accounts.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag(AccountPickerTestTags.row(account.accountIdOrId)),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                accountPickerDisplayName(account),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val balance = account.balance
            if (balance.currency.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "${balance.currency} ${balance.amount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Label fallback shared with the per-feature pickers: label, else "type ····last4". */
fun accountPickerDisplayName(account: Account): String = when {
    account.label.isNotBlank() -> account.label
    else -> {
        val type = account.typeOrProduct.ifBlank { "Account" }
        val last4 = account.accountIdOrId.filter { it.isLetterOrDigit() }.takeLast(4)
        "$type ····$last4"
    }
}

object AccountPickerTestTags {
    const val SHEET = "account_picker_sheet"
    fun row(id: String) = "account_picker_row_$id"
}
