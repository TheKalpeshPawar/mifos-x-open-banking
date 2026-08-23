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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_no_data
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_no_internet
import template.core.base.designsystem.theme.KptTheme

/** Outlined card of `label : value` rows. */
@Composable
fun MifosDetailsCard(
    keyValuePairs: Map<StringResource, String?>,
    modifier: Modifier = Modifier,
) {
    MifosCustomCard(
        variant = CardVariant.OUTLINED,
        modifier = modifier
            .fillMaxWidth()
            .border(
                DesignToken.strokes.thin,
                KptTheme.colorScheme.secondaryContainer,
                KptTheme.shapes.medium,
            ),
        shape = KptTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(KptTheme.spacing.md)) {
            keyValuePairs.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = KptTheme.spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${stringResource(key)} :",
                        style = KptTheme.typography.labelMedium,
                    )
                    Text(
                        text = value ?: "",
                        style = KptTheme.typography.labelMedium,
                        textAlign = TextAlign.Right,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MifosDetailsCardPreview() {
    MifosXOpenBankingTheme {
        MifosDetailsCard(
            keyValuePairs = mapOf(
                Res.string.core_ui_no_internet to "12345",
                Res.string.core_ui_no_data to "53736 ncc",
            ),
        )
    }
}
