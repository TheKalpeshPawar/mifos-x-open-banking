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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.generated.resources.Res
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_action_schedule
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_action_send_money
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_action_standing_order
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_action_vrp
import template.core.base.designsystem.theme.KptTheme

/** The label, icon and tap target of one quick action. */
private data class QuickAction(
    val key: String,
    val labelRes: StringResource,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * The four payment entry points, in a single card that overlaps the header. Carries no section
 * heading by design.
 */
@Composable
internal fun HomeQuickActions(
    onSendMoney: () -> Unit,
    onSchedule: () -> Unit,
    onStandingOrder: () -> Unit,
    onVrp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        QuickAction("send_money", Res.string.feature_home_action_send_money, Icons.AutoMirrored.Filled.Send, onSendMoney),
        QuickAction("schedule", Res.string.feature_home_action_schedule, Icons.Filled.CalendarMonth, onSchedule),
        QuickAction("standing_order", Res.string.feature_home_action_standing_order, Icons.Filled.Sync, onStandingOrder),
        QuickAction("vrp", Res.string.feature_home_action_vrp, Icons.Filled.Speed, onVrp),
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KptTheme.spacing.md)
            .testTag(HomeTestTags.QUICK_ACTIONS),
        shape = KptTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = KptTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = KptTheme.elevation.level2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            actions.forEach { action ->
                QuickActionItem(action = action, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickActionItem(action: QuickAction, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(KptTheme.shapes.medium)
            .clickable(onClick = action.onClick)
            .padding(vertical = KptTheme.spacing.sm)
            .testTag(HomeTestTags.quickAction(action.key)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(DesignToken.sizes.quickAction)
                .clip(DesignToken.shapes.circle)
                .background(KptTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = KptTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(DesignToken.sizes.iconLarge),
            )
        }
        Spacer(Modifier.height(KptTheme.spacing.sm))
        Text(
            text = stringResource(action.labelRes),
            style = KptTheme.typography.labelSmall,
            color = KptTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = MAX_LABEL_LINES,
        )
    }
}

private const val MAX_LABEL_LINES = 2

@Preview
@Composable
private fun HomeQuickActionsPreview() {
    MifosXOpenBankingTheme {
        HomeQuickActions(onSendMoney = {}, onSchedule = {}, onStandingOrder = {}, onVrp = {})
    }
}
