/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.account.accountDisplayName
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.generated.resources.Res
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_select_account
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_selected_account_desc
import org.mifosx.openbanking.feature.home.ui.AccountChipUi
import template.core.base.designsystem.theme.KptTheme

private val CheckIconSize = 24.dp

/**
 * Modal bottom sheet for switching the selected account, opened by tapping the hero balance card.
 *
 * This is a thin wrapper around [ModalBottomSheet] so that [AccountSelectorSheetContent] — which
 * holds every interactive surface — stays a plain composable the UI tests can render directly,
 * without reaching into the sheet's own window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSelectorSheet(
    accounts: List<AccountChipUi>,
    selectedAccountId: String,
    onSelectAccount: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        AccountSelectorSheetContent(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onSelectAccount = onSelectAccount,
        )
    }
}

/**
 * The sheet's body: one row per account, the current one marked with a check. Selecting a row emits
 * its id through [onSelectAccount]; closing the sheet afterwards is the caller's job, so this stays
 * free of sheet mechanics.
 */
@Composable
internal fun AccountSelectorSheetContent(
    accounts: List<AccountChipUi>,
    selectedAccountId: String,
    onSelectAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = KptTheme.spacing.lg)
            .testTag(HomeTestTags.ACCOUNT_SELECTOR_SHEET),
    ) {
        Text(
            text = stringResource(Res.string.feature_home_select_account),
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = KptTheme.spacing.lg,
                end = KptTheme.spacing.lg,
                bottom = KptTheme.spacing.md,
            ),
        )
        accounts.forEachIndexed { index, account ->
            val selected = account.id == selectedAccountId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected, onClick = { onSelectAccount(account.id) })
                    .padding(horizontal = KptTheme.spacing.lg, vertical = KptTheme.spacing.md)
                    .testTag(HomeTestTags.accountChip(account.id)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = accountDisplayName(
                        accountHolderName = account.accountHolderName,
                        accountSubType = account.accountSubType,
                        accountNumber = account.accountNumber,
                        rawIdentification = account.rawIdentification,
                    ),
                    style = KptTheme.typography.bodyLarge,
                    color = if (selected) {
                        KptTheme.colorScheme.primary
                    } else {
                        KptTheme.colorScheme.onSurface
                    },
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(Res.string.feature_home_selected_account_desc),
                        tint = KptTheme.colorScheme.primary,
                        modifier = Modifier.size(CheckIconSize),
                    )
                }
            }
            if (index != accounts.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
