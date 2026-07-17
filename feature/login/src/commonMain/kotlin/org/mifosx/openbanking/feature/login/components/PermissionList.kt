/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.core.model.hsbcPermission.OBPermission
import template.core.base.designsystem.theme.KptTheme

/**
 * The "data being requested" list, rendered as a single rounded surface with a divider between each
 * row — matching the mockup's `.permissions-list` card (surface-container-low, 12dp corners).
 */
@Composable
fun PermissionList(permissions: List<OBPermission>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LIST_CORNER),
        colors = CardDefaults.cardColors(
            containerColor = KptTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        permissions.forEachIndexed { index, permission ->
            if (index > 0) {
                HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
            }
            PermissionRow(permission)
        }
    }
}

@Composable
private fun PermissionRow(permission: OBPermission) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KptTheme.spacing.md, vertical = ROW_VERTICAL_PADDING)
            .testTag("login_perm_${permission.id.name}"),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            null,
            Modifier.size(20.dp),
            tint = KptTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(KptTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                permission.label,
                style = KptTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = KptTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(KptTheme.spacing.xs))
            Text(
                permission.description,
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val LIST_CORNER = 12.dp
private val ROW_VERTICAL_PADDING = 12.dp
