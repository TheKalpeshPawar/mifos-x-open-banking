/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.transactions.generated.resources.Res
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_body_consent_withdrawn
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_body_network
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_body_rate_limited
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_body_session_expired
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_consent_hint
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_retry
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_status_consent_withdrawn
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_status_network
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_status_rate_limited
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_status_session_expired
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_error_title
import org.mifosx.openbanking.feature.transactions.ui.TransactionsErrorKind
import template.core.base.designsystem.theme.KptTheme

private val ErrorCircleSize = 96.dp
private val ErrorIconSize = 40.dp
private val StatusIconSize = 16.dp

/** Error state: the transactions could not be loaded — shows the HTTP status and offers a retry. */
@Composable
internal fun TransactionsError(
    kind: TransactionsErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(KptTheme.spacing.lg)
            .testTag(TransactionsTestTags.ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier.size(ErrorCircleSize).clip(CircleShape).background(KptTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = KptTheme.colorScheme.error,
                modifier = Modifier.size(ErrorIconSize),
            )
        }
        StatusChip(kind)
        Text(
            text = stringResource(Res.string.feature_transactions_error_title),
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(kind.bodyRes()),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.feature_transactions_error_consent_hint),
            style = KptTheme.typography.bodySmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (kind.recoverable) {
            MifosFilledPillButton(
                label = stringResource(Res.string.feature_transactions_error_retry),
                onClick = onRetry,
                testTag = TransactionsTestTags.ERROR_RETRY,
            )
        }
    }
}

@Composable
private fun StatusChip(kind: TransactionsErrorKind) {
    Surface(
        color = KptTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(percent = 50),
        modifier = Modifier.testTag(TransactionsTestTags.ERROR_STATUS),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            modifier = Modifier.padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = KptTheme.colorScheme.error,
                modifier = Modifier.size(StatusIconSize),
            )
            Text(
                text = stringResource(kind.statusRes()),
                style = KptTheme.typography.labelMedium,
                color = KptTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

private fun TransactionsErrorKind.statusRes(): StringResource = when (this) {
    TransactionsErrorKind.SESSION_EXPIRED -> Res.string.feature_transactions_error_status_session_expired
    TransactionsErrorKind.CONSENT_WITHDRAWN -> Res.string.feature_transactions_error_status_consent_withdrawn
    TransactionsErrorKind.RATE_LIMITED -> Res.string.feature_transactions_error_status_rate_limited
    TransactionsErrorKind.NETWORK -> Res.string.feature_transactions_error_status_network
}

private fun TransactionsErrorKind.bodyRes(): StringResource = when (this) {
    TransactionsErrorKind.SESSION_EXPIRED -> Res.string.feature_transactions_error_body_session_expired
    TransactionsErrorKind.CONSENT_WITHDRAWN -> Res.string.feature_transactions_error_body_consent_withdrawn
    TransactionsErrorKind.RATE_LIMITED -> Res.string.feature_transactions_error_body_rate_limited
    TransactionsErrorKind.NETWORK -> Res.string.feature_transactions_error_body_network
}
