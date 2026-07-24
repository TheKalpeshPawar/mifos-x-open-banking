/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.consentdetail.generated.resources.Res
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_error_a11y
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_error_network
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_error_not_found
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_error_revoked
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_error_server
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_error_title
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_error_token_expired
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_retry
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailErrorKind

private val IconWellSize = 80.dp
private val IconSize = 36.dp
private val BodyMaxWidth = 300.dp
private val ContentPadding = 24.dp
private val LineGap = 12.dp

/**
 * Error state: a filled error well, the failure title, the message for this specific failure, and
 * Retry.
 *
 * Reached from a failed load and from a failed revoke alike — in the second case the local session
 * is deliberately still intact, so retrying is a real option rather than a dead end.
 */
@Composable
internal fun ConsentDetailError(
    kind: ConsentDetailErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_consent_detail_error_a11y)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(ConsentDetailTestTags.ERROR_STATE)
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
            text = stringResource(Res.string.feature_consent_detail_error_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(ConsentDetailTestTags.ERROR_TITLE),
        )
        Text(
            text = stringResource(kind.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(ConsentDetailTestTags.ERROR_BODY),
        )
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_consent_detail_retry),
            onClick = onRetry,
            testTag = ConsentDetailTestTags.RETRY_BUTTON,
        )
    }
}

private fun ConsentDetailErrorKind.bodyResource(): StringResource = when (this) {
    ConsentDetailErrorKind.ConsentNotFound -> Res.string.feature_consent_detail_error_not_found
    ConsentDetailErrorKind.ConsentRevoked -> Res.string.feature_consent_detail_error_revoked
    ConsentDetailErrorKind.TokenExpired -> Res.string.feature_consent_detail_error_token_expired
    ConsentDetailErrorKind.NetworkError -> Res.string.feature_consent_detail_error_network
    ConsentDetailErrorKind.RevokeServerError -> Res.string.feature_consent_detail_error_server
}
