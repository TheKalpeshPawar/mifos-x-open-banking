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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** A table row of fixed-width cells separated by hairline dividers. */
@Composable
fun MifosTableRow(
    cells: List<@Composable () -> Unit>,
    widths: List<Dp>,
    modifier: Modifier = Modifier,
    edgeOffset: Dp = 0.dp,
    backgroundColor: Color = KptTheme.colorScheme.surfaceVariant,
    cornerShape: Shape = RectangleShape,
    showTopBorder: Boolean = false,
    showBottomBorder: Boolean = true,
    showSideBorders: Boolean = true,
    borderColor: Color = KptTheme.colorScheme.surfaceVariant,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (showTopBorder) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = edgeOffset),
                thickness = DesignToken.strokes.thin,
                color = borderColor,
            )
        }
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = edgeOffset)
                .clip(cornerShape)
                .background(backgroundColor)
                .clickable { onClick() },
        ) {
            cells.forEachIndexed { index, content ->
                val cellWidth = widths.getOrElse(index) { DesignToken.sizes.imageLarge }
                if (showSideBorders) {
                    VerticalDivider(
                        thickness = DesignToken.strokes.thin,
                        color = borderColor,
                    )
                }
                Box(modifier = Modifier.width(cellWidth)) {
                    content()
                    if (showBottomBorder) {
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.BottomStart),
                            thickness = DesignToken.strokes.thin,
                            color = borderColor,
                        )
                    }
                }
                if (showSideBorders && index == cells.lastIndex) {
                    VerticalDivider(
                        thickness = DesignToken.strokes.thin,
                        color = borderColor,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MifosTableRowPreview() {
    MifosXOpenBankingTheme {
        MifosTableRow(
            cells = listOf({ Text(text = "Cell 1") }, { Text(text = "Cell 2") }),
            widths = listOf(DesignToken.sizes.imageLarge, DesignToken.sizes.imageLarge),
        )
    }
}
