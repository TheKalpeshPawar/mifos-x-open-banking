/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.accountdetail.generated.resources.Res
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_error_consent_withdrawn
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_error_network
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_error_not_found
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_error_title
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_error_token_expired
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_error_unexpected
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_retry
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailErrorKind

private val IconWellSize = 72.dp
private val IconSize = 40.dp
private val BodyMaxWidth = 280.dp

/**
 * Error state: a tinted icon well, the failure title, the message for this specific failure, and a
 * Retry button.
 *
 * Retry is shown only when the failure can actually be recovered by retrying. A withdrawn consent
 * or an account outside the authorised set will fail identically on every attempt, so offering the
 * button there would be a lie; those cases leave back-navigation as the way out.
 */
@Composable
internal fun AccountDetailError(
    kind: AccountDetailErrorKind,
    showRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(AccountDetailTestTags.ERROR_STATE),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconWellSize)
                .background(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = stringResource(Res.string.feature_account_detail_error_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(AccountDetailTestTags.ERROR_TITLE),
        )
        Text(
            text = stringResource(kind.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(AccountDetailTestTags.ERROR_BODY),
        )
        if (showRetry) {
            MifosFilledPillButton(
                label = stringResource(Res.string.feature_account_detail_retry),
                onClick = onRetry,
                testTag = AccountDetailTestTags.RETRY_BUTTON,
            )
        }
    }
}

private fun AccountDetailErrorKind.bodyResource(): StringResource = when (this) {
    AccountDetailErrorKind.TokenExpired -> Res.string.feature_account_detail_error_token_expired
    AccountDetailErrorKind.ConsentWithdrawn -> Res.string.feature_account_detail_error_consent_withdrawn
    AccountDetailErrorKind.AccountNotFound -> Res.string.feature_account_detail_error_not_found
    AccountDetailErrorKind.Network -> Res.string.feature_account_detail_error_network
    AccountDetailErrorKind.Unexpected -> Res.string.feature_account_detail_error_unexpected
}
