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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/**
 * A full-width, pill-shaped primary button — the app's main call-to-action style.
 *
 * @param label Text shown on the button.
 * @param onClick Invoked when the button is tapped.
 * @param modifier Modifier applied to the button.
 * @param icon Optional leading icon rendered before [label].
 * @param testTag Optional UI-test tag applied to the button.
 */
@Composable
fun MifosFilledPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT)
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
        shape = RoundedCornerShape(PILL_CORNER_PERCENT),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(ICON_SIZE))
            Spacer(Modifier.width(KptTheme.spacing.sm))
        }
        Text(label, style = KptTheme.typography.labelLarge)
    }
}

private val BUTTON_HEIGHT = 48.dp
private val ICON_SIZE = 18.dp
private const val PILL_CORNER_PERCENT = 50

@Preview
@Composable
private fun MifosFilledPillButtonPreview() {
    MifosXOpenBankingTheme {
        MifosFilledPillButton(
            label = "Continue with HSBC",
            onClick = {},
        )
    }
}
