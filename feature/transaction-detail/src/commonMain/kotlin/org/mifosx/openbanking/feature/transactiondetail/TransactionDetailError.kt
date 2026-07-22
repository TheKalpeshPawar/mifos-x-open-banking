/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.Res
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_accessibility
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_consent_withdrawn
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_network
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_title
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_token_expired
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_unexpected
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_go_back
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_retry
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailErrorKind

private val IconWellSize = 80.dp
private val IconSize = 40.dp
private val BodyMaxWidth = 280.dp
private val ContentPadding = 24.dp
private val LineGap = 8.dp
private val ButtonHeight = 48.dp
private const val PILL_CORNER_PERCENT = 50

/**
 * Error state: a tinted icon well, the failure title, the message for this failure, and one action.
 *
 * A recoverable failure (an expired token, a dropped connection) offers a filled Retry; a
 * non-recoverable one (a withdrawn consent) offers an outlined Go Back, because retrying would fail
 * identically and back-navigation is the only way out.
 */
@Composable
internal fun TransactionDetailError(
    kind: TransactionDetailErrorKind,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_transaction_detail_error_accessibility)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(TransactionDetailTestTags.ERROR_STATE)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
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
            text = stringResource(Res.string.feature_transaction_detail_error_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TransactionDetailTestTags.ERROR_TITLE),
        )
        Text(
            text = stringResource(kind.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(TransactionDetailTestTags.ERROR_BODY),
        )
        if (kind.recoverable) {
            MifosFilledPillButton(
                label = stringResource(Res.string.feature_transaction_detail_retry),
                onClick = onRetry,
                testTag = TransactionDetailTestTags.RETRY_BUTTON,
            )
        } else {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(PILL_CORNER_PERCENT),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeight)
                    .testTag(TransactionDetailTestTags.GO_BACK_BUTTON),
            ) {
                Text(
                    text = stringResource(Res.string.feature_transaction_detail_go_back),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun TransactionDetailErrorKind.bodyResource(): StringResource = when (this) {
    TransactionDetailErrorKind.TokenExpired -> Res.string.feature_transaction_detail_error_token_expired
    TransactionDetailErrorKind.ConsentWithdrawn -> Res.string.feature_transaction_detail_error_consent_withdrawn
    TransactionDetailErrorKind.Network -> Res.string.feature_transaction_detail_error_network
    TransactionDetailErrorKind.Unexpected -> Res.string.feature_transaction_detail_error_unexpected
}
