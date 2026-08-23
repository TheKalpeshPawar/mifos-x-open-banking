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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** Clickable elevated card with a content slot. */
@Composable
fun MifosItemCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: Dp = KptTheme.elevation.level1,
    shape: Shape = KptTheme.shapes.small,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = shape,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content,
    )
}

@Preview
@Composable
private fun MifosItemCardPreview() {
    MifosXOpenBankingTheme {
        MifosItemCard(onClick = {}) { Text("Card Content") }
    }
}
