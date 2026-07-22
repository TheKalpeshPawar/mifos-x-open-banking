/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.generated.resources.Res
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_action_consents
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_action_pay
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_action_statements
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_action_transactions
import template.core.base.designsystem.theme.KptTheme

private val ActionIconSize = 24.dp
private const val DISABLED_ALPHA = 0.4f

/**
 * Row of primary account actions. Pay is disabled (payment initiation is not yet available), and
 * Statements is disabled unless [statementsEnabled] — HSBC exposes statements on credit cards only;
 * the others navigate via their callbacks.
 */
@Composable
internal fun QuickActionsRow(
    onTransactions: () -> Unit,
    onStatements: () -> Unit,
    onConsents: () -> Unit,
    statementsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(KptTheme.colorScheme.surfaceContainerHigh, KptTheme.shapes.large)
            .padding(vertical = KptTheme.spacing.md)
            .testTag(HomeTestTags.QUICK_ACTIONS),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        QuickAction(
            icon = Icons.Filled.Payments,
            label = stringResource(Res.string.feature_home_action_pay),
            enabled = false,
            onClick = {},
        )
        QuickAction(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            label = stringResource(Res.string.feature_home_action_transactions),
            enabled = true,
            onClick = onTransactions,
        )
        QuickAction(
            icon = Icons.Filled.Description,
            label = stringResource(Res.string.feature_home_action_statements),
            enabled = statementsEnabled,
            onClick = onStatements,
        )
        QuickAction(
            icon = Icons.Filled.Shield,
            label = stringResource(Res.string.feature_home_action_consents),
            enabled = true,
            onClick = onConsents,
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (enabled) {
        KptTheme.colorScheme.primary
    } else {
        KptTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
    }
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(KptTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(ActionIconSize))
        Text(text = label, style = KptTheme.typography.labelSmall, color = tint)
    }
}
