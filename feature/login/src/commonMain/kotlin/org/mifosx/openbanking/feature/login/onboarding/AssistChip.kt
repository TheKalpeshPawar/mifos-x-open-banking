/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import template.core.base.designsystem.theme.KptTheme

@Composable
fun AssistChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(KptTheme.spacing.sm)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(1.dp, KptTheme.colorScheme.outline, shape)
            .padding(horizontal = KptTheme.spacing.md),
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = KptTheme.colorScheme.primary)
        Spacer(Modifier.width(KptTheme.spacing.sm))
        Text(
            label,
            style = KptTheme.typography.labelLarge,
            color = KptTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        if (onClick != null) {
            Spacer(Modifier.width(KptTheme.spacing.sm))
            Icon(
                Icons.Outlined.OpenInNew,
                null,
                Modifier.size(14.dp),
                tint = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
