/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_apply
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_filter
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_reset
import template.core.base.designsystem.theme.KptTheme

/** A filter header with dismiss, reset and apply actions. */
@Composable
fun FilterTopSection(
    isAnyFilterSelected: Boolean,
    resetFilters: () -> Unit,
    onApplyFilter: () -> Unit,
    dismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resetColor = if (isAnyFilterSelected) {
        KptTheme.colorScheme.primary
    } else {
        KptTheme.colorScheme.inversePrimary
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            Icon(
                modifier = Modifier
                    .size(DesignToken.sizes.iconSmall)
                    .clickable { dismissDialog() },
                imageVector = AppIcons.Close,
                contentDescription = null,
            )
            Text(
                text = stringResource(Res.string.core_ui_filter),
                style = KptTheme.typography.titleMedium,
                color = KptTheme.colorScheme.onBackground,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.clickable(isAnyFilterSelected) { resetFilters() },
                horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.core_ui_reset),
                    style = KptTheme.typography.bodySmall,
                    color = resetColor,
                )
                Icon(
                    modifier = Modifier.size(DesignToken.sizes.iconSmall),
                    imageVector = AppIcons.Refresh,
                    contentDescription = null,
                    tint = resetColor,
                )
            }

            Row(
                modifier = Modifier.clickable { onApplyFilter() },
                horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.core_ui_apply),
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.primary,
                )
                Icon(
                    modifier = Modifier.size(DesignToken.sizes.iconSmall),
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview
@Composable
private fun FilterTopSectionPreview() {
    MifosXOpenBankingTheme {
        FilterTopSection(
            isAnyFilterSelected = true,
            resetFilters = {},
            onApplyFilter = {},
            dismissDialog = {},
        )
    }
}
