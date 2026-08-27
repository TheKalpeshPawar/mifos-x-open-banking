/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.common.formatAccountIdentifier
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_picker_available_balance
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_picker_choose
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_picker_collapse
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_picker_expand
import template.core.base.designsystem.theme.KptTheme

private val RowMinHeight = DesignToken.sizes.rowTall

/** Matches the outlined text fields the picker sits beside. */
private val BorderThickness = DesignToken.strokes.hairline

/**
 * An account chooser: one collapsed row that opens into the alternatives.
 *
 * Each row reads as the masked account number, the account type beneath it, and the balance. The
 * chosen account stays visible while the rest fold away, so a decision made once and then only
 * looked at does not hold the screen open.
 *
 * @param options The accounts to offer, each paired with its resolved balance (or null when the
 *   balance could not be fetched).
 * @param selectedId The chosen account, or null when none has been chosen yet.
 * @param extraOptions Rendered inside the expansion after the accounts, for alternatives that are
 *   not accounts — deferring the choice to the bank, say.
 */
@Composable
fun MifosAccountPicker(
    options: List<AccountWithBalance>,
    selectedId: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    unselectedLabel: String? = null,
    unselectedSupporting: String = "",
    headerTestTag: String? = null,
    listTestTag: String? = null,
    rowTestTag: (String) -> String = ::accountPickerRowTag,
    extraOptions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(KptTheme.shapes.medium)
            .border(
                width = BorderThickness,
                color = KptTheme.colorScheme.primary,
                shape = KptTheme.shapes.medium,
            )
            .background(KptTheme.colorScheme.surfaceContainerLowest),
    ) {
        CollapsedRow(
            option = options.firstOrNull { it.account.accountId == selectedId },
            expanded = expanded,
            onToggle = onToggle,
            unselectedLabel = unselectedLabel ?: stringResource(Res.string.core_ui_account_picker_choose),
            unselectedSupporting = unselectedSupporting,
            testTag = headerTestTag,
        )

        if (expanded) {
            HorizontalDivider(color = KptTheme.colorScheme.primary)
            Column(modifier = if (listTestTag == null) Modifier else Modifier.testTag(listTestTag)) {
                options.forEach { option ->
                    AccountRow(
                        option = option,
                        selected = option.account.accountId == selectedId,
                        onClick = { onSelect(option.account.accountId) },
                        testTag = rowTestTag(option.account.accountId),
                    )
                }
                extraOptions?.invoke(this)
            }
        }
    }
}

/** What the picker says while it is shut: the chosen account, or an invitation to choose one. */
@Composable
private fun CollapsedRow(
    option: AccountWithBalance?,
    expanded: Boolean,
    onToggle: () -> Unit,
    unselectedLabel: String,
    unselectedSupporting: String,
    testTag: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .selectable(selected = false, role = Role.Button, onClick = onToggle)
            .padding(KptTheme.spacing.md)
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (option == null) {
                AccountLines(headline = unselectedLabel, type = unselectedSupporting, balance = "")
            } else {
                option.Lines()
            }
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = stringResource(
                if (expanded) {
                    Res.string.core_ui_account_picker_collapse
                } else {
                    Res.string.core_ui_account_picker_expand
                },
            ),
            tint = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountRow(
    option: AccountWithBalance,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = RowMinHeight)
            .padding(KptTheme.spacing.md)
            .testTag(testTag),
        verticalArrangement = Arrangement.Center,
    ) {
        option.Lines(emphasised = selected)
    }
}

/** The three lines an account reads as: masked number, type, then balance. */
@Composable
private fun AccountWithBalance.Lines(emphasised: Boolean = false) {
    AccountLines(
        name = account.accountHolderName,
        headline = formatAccountIdentifier(account.scheme, account.identification),
        type = accountTypeLabel(account.accountTypeCode, account.description),
        balance = balance?.let { formatMoney(it.availableAmount, it.currency) }.orEmpty(),
        emphasised = emphasised,
    )
}

@Composable
private fun AccountLines(
    headline: String,
    type: String,
    balance: String,
    name: String = "",
    emphasised: Boolean = false,
) {
    if (name.isNotBlank()) {
        Text(
            text = name,
            style = KptTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasised) KptTheme.colorScheme.primary else KptTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Text(
        text = headline,
        style = if (name.isNotBlank()) KptTheme.typography.bodyMedium else KptTheme.typography.titleMedium,
        fontWeight = if (name.isNotBlank()) null else FontWeight.SemiBold,
        color = when {
            name.isNotBlank() -> KptTheme.colorScheme.onSurfaceVariant
            emphasised -> KptTheme.colorScheme.primary
            else -> KptTheme.colorScheme.onSurface
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (type.isNotBlank()) {
        Text(
            text = type,
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (balance.isNotBlank()) {
        Text(
            text = stringResource(Res.string.core_ui_account_picker_available_balance, balance),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The node tag one account row carries, so a suite can select a known account. */
fun accountPickerRowTag(accountId: String): String = "mifosAccountPicker:row:$accountId"
