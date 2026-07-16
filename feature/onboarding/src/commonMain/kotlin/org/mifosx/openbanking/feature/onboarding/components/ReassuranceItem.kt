/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import template.core.base.designsystem.theme.KptTheme

@Composable
fun ReassuranceItem(
    icon: ImageVector,
    headline: String,
    body: String,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = KptTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(KptTheme.spacing.sm)).background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(20.dp), tint = iconTint)
            }
            Spacer(Modifier.width(KptTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(headline, style = KptTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = KptTheme.colorScheme.onSurface)
                Spacer(Modifier.height(KptTheme.spacing.xs))
                Text(body, style = KptTheme.typography.bodyMedium, color = KptTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
    }
}
