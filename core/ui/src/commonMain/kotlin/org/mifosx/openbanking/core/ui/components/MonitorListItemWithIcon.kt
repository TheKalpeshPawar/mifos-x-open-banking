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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_retry
import template.core.base.designsystem.theme.KptTheme

/** A list row with a round icon, a title and a subtitle. */
@Composable
fun MonitorListItemWithIcon(
    titleId: StringResource,
    subTitleId: StringResource,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(KptTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MifosRoundIcon(
            icon = icon,
            modifier = Modifier.size(DesignToken.sizes.iconExtraLarge),
        )
        Spacer(modifier = Modifier.width(KptTheme.spacing.sm))
        Column {
            Text(
                text = stringResource(titleId),
                style = KptTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(subTitleId),
                style = KptTheme.typography.bodyMedium,
                modifier = Modifier
                    .alpha(0.7f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun MonitorListItemWithIconPreview() {
    MifosXOpenBankingTheme {
        MonitorListItemWithIcon(
            titleId = Res.string.core_ui_retry,
            subTitleId = Res.string.core_ui_retry,
            icon = AppIcons.Check,
            onClick = {},
        )
    }
}
