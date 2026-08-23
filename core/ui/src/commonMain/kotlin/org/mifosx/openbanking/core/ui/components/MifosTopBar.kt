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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** Top app bar with a back navigation icon and title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosTopBar(
    topBarTitle: String,
    onNavigationIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(text = topBarTitle, style = KptTheme.typography.titleMedium)
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(imageVector = AppIcons.Back, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = KptTheme.colorScheme.surface,
        ),
        actions = actions,
        modifier = modifier,
    )
}

/** Top app bar with an optional back navigation icon and title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosTopBar(
    topBarTitle: String,
    showNavigationIcon: Boolean,
    modifier: Modifier = Modifier,
    onNavigationIconClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(text = topBarTitle, style = KptTheme.typography.titleMedium)
        },
        navigationIcon = {
            if (showNavigationIcon) {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(imageVector = AppIcons.Back, contentDescription = "Back")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = KptTheme.colorScheme.surface,
        ),
        actions = actions,
        modifier = modifier,
    )
}

/** Top app bar with rounded bottom corners and an optional brand icon or back button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosRoundedTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    brandIcon: ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            if (brandIcon == null) {
                Text(
                    text = title,
                    style = KptTheme.typography.titleMedium,
                    color = KptTheme.colorScheme.onBackground,
                )
            }
        },
        actions = actions,
        navigationIcon = {
            if (brandIcon != null) {
                Box(modifier = Modifier.padding(KptTheme.spacing.md)) {
                    Icon(
                        imageVector = brandIcon,
                        contentDescription = "Brand Icon",
                        modifier = Modifier
                            .size(DesignToken.sizes.badge, DesignToken.sizes.imageWide)
                            .align(Alignment.TopStart),
                    )
                }
            } else {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = AppIcons.Back, contentDescription = null)
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = KptTheme.colorScheme.surface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(KptTheme.shapes.large)
            .background(KptTheme.colorScheme.surface),
    )
}

@Preview
@Composable
private fun MifosRoundedTopAppBarPreview() {
    MifosXOpenBankingTheme {
        MifosRoundedTopAppBar(title = "TopAppBar", onNavigateBack = {})
    }
}

@Preview
@Composable
private fun MifosTopBarPreview() {
    MifosXOpenBankingTheme {
        MifosTopBar(topBarTitle = "Title", onNavigationIconClick = {})
    }
}
