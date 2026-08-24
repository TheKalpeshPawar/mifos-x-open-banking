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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.generated.resources.Res
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_account_icon_desc
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_available
import template.core.base.designsystem.theme.KptTheme

private val HeroCardHeight = 180.dp
private val BankIconSize = 24.dp

/**
 * Primary account card: account type, display name, current balance, available balance, and the
 * identification. All values arrive pre-formatted; tapping opens the account selector sheet via
 * [onClick].
 */
@Composable
internal fun HeroBalanceCard(
    accountTypeLabel: String,
    displayName: String,
    balanceLabel: String,
    availableAmountLabel: String,
    accountNumberLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(HeroCardHeight)
            .background(KptTheme.colorScheme.primaryContainer, KptTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(KptTheme.spacing.lg)
            .testTag(HomeTestTags.HERO_CARD),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = accountTypeLabel,
                style = KptTheme.typography.labelMedium,
                color = KptTheme.colorScheme.onPrimaryContainer,
            )
            Icon(
                imageVector = Icons.Filled.AccountBalance,
                contentDescription = stringResource(Res.string.feature_home_account_icon_desc),
                tint = KptTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(BankIconSize),
            )
        }
        Text(
            text = displayName,
            style = KptTheme.typography.titleLarge,
            color = KptTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = balanceLabel,
            style = KptTheme.typography.displaySmall,
            color = KptTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(Res.string.feature_home_available, availableAmountLabel),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onPrimaryContainer,
        )
        if (accountNumberLabel.isNotBlank()) {
            Text(
                text = accountNumberLabel,
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
