/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.accounts.AccountsTestTags
import org.mifosx.openbanking.feature.accounts.generated.resources.Res
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_consent_expiring_body
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_consent_expiring_title
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_consent_reconfirm
import template.core.base.designsystem.theme.KptTheme

/**
 * Sticky warning banner shown when the OBIE consent is within its expiry window. Offers a reconfirm
 * action so the PSU can re-extend access before it lapses.
 */
@Composable
internal fun ConsentExpiryBanner(
    daysRemaining: Int,
    onReconfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(AccountsTestTags.CONSENT_BANNER),
        color = KptTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = KptTheme.colorScheme.onTertiaryContainer,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(Res.string.feature_accounts_consent_expiring_title),
                    style = KptTheme.typography.titleSmall,
                    color = KptTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = stringResource(Res.string.feature_accounts_consent_expiring_body, daysRemaining),
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.onTertiaryContainer,
                )
            }
            TextButton(
                onClick = onReconfirm,
                modifier = Modifier.testTag(AccountsTestTags.CONSENT_RECONFIRM),
            ) {
                Text(stringResource(Res.string.feature_accounts_consent_reconfirm))
            }
        }
    }
}
