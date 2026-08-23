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

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme

/** An icon button that opens a drop-down of labelled actions. */
@Composable
fun MifosDropdownMenu(
    menuItems: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
    menuIcon: ImageVector = AppIcons.MoreVert,
    menuIconContentDescription: String = "Menu",
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { showMenu = !showMenu }) {
            Icon(
                imageVector = menuIcon,
                contentDescription = menuIconContentDescription,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            menuItems.forEach { (title, action) ->
                DropdownMenuItem(
                    text = { Text(text = title) },
                    onClick = {
                        showMenu = false
                        action()
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun MifosDropdownMenuPreview() {
    MifosXOpenBankingTheme {
        MifosDropdownMenu(
            menuItems = listOf(
                "Edit" to {},
                "Delete" to {},
            ),
        )
    }
}
