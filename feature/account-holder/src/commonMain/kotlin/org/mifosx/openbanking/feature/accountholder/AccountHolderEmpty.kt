/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.accountholder.generated.resources.Res
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_empty_accessibility
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_empty_body
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_empty_title

private val IconSize = 64.dp
private val BodyMaxWidth = 320.dp
private val ContentPadding = 24.dp
private val LineGap = 16.dp
private const val ICON_ALPHA = 0.6f

/**
 * Empty state: the bank answered without an identity for this account.
 *
 * Identity-only screen, so there is no action offered here — consent re-authorisation lives in
 * Settings → Consents. The message tells the user the bank returned no holder record.
 */
@Composable
internal fun AccountHolderEmpty(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_account_holder_empty_accessibility)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(AccountHolderTestTags.EMPTY_STATE)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.NoAccounts,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize).alpha(ICON_ALPHA),
        )
        Text(
            text = stringResource(Res.string.feature_account_holder_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(AccountHolderTestTags.EMPTY_TITLE),
        )
        Text(
            text = stringResource(Res.string.feature_account_holder_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(AccountHolderTestTags.EMPTY_BODY),
        )
    }
}
