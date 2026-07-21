/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.accountdetail.AccountDetailTestTags
import org.mifosx.openbanking.feature.accountdetail.generated.resources.Res
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_open_banking_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_open_banking_badge

private val BADGE_RADIUS = 12.dp
private val DOT_SIZE = 8.dp

/**
 * Non-interactive assurance strip telling the user this data arrived over regulated UK Open
 * Banking. Outlined rather than filled so it reads as a note, not an action.
 */
@Composable
internal fun OpenBankingBadge(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_account_detail_open_banking_accessibility)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(BADGE_RADIUS),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(BADGE_RADIUS),
            )
            .testTag(AccountDetailTestTags.OPEN_BANKING_BADGE)
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DOT_SIZE),
        ) {
            Spacer(
                modifier = Modifier
                    .size(DOT_SIZE)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
            )
            Text(
                text = stringResource(Res.string.feature_account_detail_open_banking_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
