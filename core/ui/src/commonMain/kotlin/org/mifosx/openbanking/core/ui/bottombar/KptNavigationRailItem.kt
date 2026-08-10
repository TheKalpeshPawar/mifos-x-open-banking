/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.bottombar

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import template.core.base.designsystem.theme.KptTheme

@Composable
fun ColumnScope.KptNavigationRailItem(
    contentDescriptionRes: StringResource,
    selectedIconRes: ImageVector,
    unselectedIconRes: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRailItem(
        icon = {
            Icon(
                imageVector = if (isSelected) selectedIconRes else unselectedIconRes,
                contentDescription = stringResource(contentDescriptionRes),
            )
        },
        selected = isSelected,
        onClick = onClick,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = KptTheme.colorScheme.onPrimaryContainer,
            unselectedIconColor = KptTheme.colorScheme.outline,
        ),
        modifier = modifier,
    )
}
