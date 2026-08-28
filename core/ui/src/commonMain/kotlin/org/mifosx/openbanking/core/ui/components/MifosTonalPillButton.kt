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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/**
 * A full-width, pill-shaped secondary button — the companion to [MifosFilledPillButton].
 *
 * Used where an action is offered alongside or instead of the primary one and should read as the
 * quieter of the two: the beneficiaries screen's "View Consents" recovery, and consent-detail's
 * "Reconfirm before expiry". It renders as an outlined button rather than the primary fill, so a
 * destructive or corrective route never competes with the main call to action.
 *
 * @param label Text shown on the button.
 * @param onClick Invoked when the button is tapped.
 * @param modifier Modifier applied to the button.
 * @param icon Optional leading icon rendered before [label].
 * @param testTag Optional UI-test tag applied to the button.
 * @param enabled Whether the button accepts taps. A false value both greys the button and stops it
 *   responding, so a control that cannot act never looks as though it can.
 */
@Composable
fun MifosTonalPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String? = null,
    enabled: Boolean = true,
) {
    MifosOutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
        shape = DesignToken.shapes.pill,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(DesignToken.sizes.iconExtraSmall))
            Spacer(Modifier.width(KptTheme.spacing.sm))
        }
        Text(label, style = KptTheme.typography.labelLarge)
    }
}

@Preview
@Composable
private fun MifosTonalPillButtonPreview() {
    MifosXOpenBankingTheme {
        MifosTonalPillButton(
            label = "View Consents",
            onClick = {},
        )
    }
}
