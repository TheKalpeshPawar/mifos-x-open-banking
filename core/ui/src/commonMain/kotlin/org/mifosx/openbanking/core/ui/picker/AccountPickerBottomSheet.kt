/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.mifosx.openbanking.core.model.obp.Account

/**
 * Reusable account picker bottom sheet — an [Account]-typed front for [PickerBottomSheet].
 * Lists [accounts] with the shared row treatment (label, balance as supporting text,
 * check on the row matching [selectedAccountId]). Selecting a row reports the account
 * and the caller dismisses; swiping down / tapping outside calls [onDismiss].
 *
 * When [allOptionLabel] and [onAllSelected] are both provided, an aggregate row renders
 * above the account list; it carries the check when [selectedAccountId] is blank.
 */
@Composable
fun AccountPickerBottomSheet(
    accounts: List<Account>,
    selectedAccountId: String,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Choose account",
    allOptionLabel: String? = null,
    onAllSelected: (() -> Unit)? = null,
) {
    val options = buildList {
        if (allOptionLabel != null && onAllSelected != null) {
            add(
                PickerSheetOption(
                    id = "",
                    label = allOptionLabel,
                    testTag = AccountPickerTestTags.ALL_ROW,
                ),
            )
        }
        accounts.forEach { account ->
            add(
                PickerSheetOption(
                    id = account.accountIdOrId,
                    label = accountPickerDisplayName(account),
                    supportingText = account.balance
                        .takeIf { it.currency.isNotBlank() }
                        ?.let { "${it.currency} ${it.amount}" }
                        .orEmpty(),
                    testTag = AccountPickerTestTags.row(account.accountIdOrId),
                ),
            )
        }
    }
    PickerBottomSheet(
        options = options,
        selectedId = selectedAccountId,
        onOptionSelected = { option ->
            if (option.id.isBlank()) {
                onAllSelected?.invoke()
            } else {
                accounts.firstOrNull { it.accountIdOrId == option.id }?.let(onAccountSelected)
            }
        },
        onDismiss = onDismiss,
        title = title,
        modifier = modifier,
        sheetTestTag = AccountPickerTestTags.SHEET,
    )
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
    const val ALL_ROW = "account_picker_row_all"
    fun row(id: String) = "account_picker_row_$id"
}
