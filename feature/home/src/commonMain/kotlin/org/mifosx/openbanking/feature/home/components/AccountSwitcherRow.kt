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

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.ui.AccountChipUi
import template.core.base.designsystem.theme.KptTheme

/**
 * Horizontal, scrollable row of account chips. The chip whose id matches [selectedAccountId] is
 * highlighted; tapping a chip emits its id through [onSelectAccount].
 */
@Composable
internal fun AccountSwitcherRow(
    accounts: List<AccountChipUi>,
    selectedAccountId: String,
    onSelectAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        accounts.forEach { chip ->
            val selected = chip.id == selectedAccountId
            val background =
                if (selected) KptTheme.colorScheme.secondaryContainer else KptTheme.colorScheme.surfaceContainerHigh
            val foreground =
                if (selected) KptTheme.colorScheme.onSecondaryContainer else KptTheme.colorScheme.onSurfaceVariant
            Text(
                text = chip.nickname,
                style = KptTheme.typography.labelLarge,
                color = foreground,
                modifier = Modifier
                    .selectable(selected = selected, onClick = { onSelectAccount(chip.id) })
                    .background(background, KptTheme.shapes.large)
                    .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.sm)
                    .testTag(HomeTestTags.accountChip(chip.id)),
            )
        }
    }
}
