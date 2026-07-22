/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements

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
import org.mifosx.openbanking.feature.statements.generated.resources.Res
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_accessibility
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_consent_scope
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_network
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_rate_limited
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_server
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_title
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_token_expired
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_retry
import org.mifosx.openbanking.feature.statements.ui.StatementsErrorKind

private val IconWellSize = 80.dp
private val IconSize = 40.dp
private val BodyMaxWidth = 280.dp
private val ContentPadding = 24.dp
private val LineGap = 8.dp

/**
 * Error state: a tinted icon well, the failure title, the message for this specific failure, and a
 * Retry button.
 *
 * Unlike direct-debits, every statements failure is retriable — a consent-scope refusal is recovered
 * by re-authorising in Consents and trying again — so Retry always appears.
 */
@Composable
internal fun StatementsError(
    kind: StatementsErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_statements_error_accessibility)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(StatementsTestTags.ERROR_STATE)
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
            text = stringResource(Res.string.feature_statements_error_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(StatementsTestTags.ERROR_TITLE),
        )
        Text(
            text = stringResource(kind.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(StatementsTestTags.ERROR_BODY),
        )
        if (kind.isRetriable) {
            MifosFilledPillButton(
                label = stringResource(Res.string.feature_statements_retry),
                onClick = onRetry,
                testTag = StatementsTestTags.RETRY_BUTTON,
            )
        }
    }
}

private fun StatementsErrorKind.bodyResource(): StringResource = when (this) {
    StatementsErrorKind.TokenExpired -> Res.string.feature_statements_error_token_expired
    StatementsErrorKind.ConsentScope -> Res.string.feature_statements_error_consent_scope
    StatementsErrorKind.RateLimited -> Res.string.feature_statements_error_rate_limited
    StatementsErrorKind.ServerError -> Res.string.feature_statements_error_server
    StatementsErrorKind.NetworkError -> Res.string.feature_statements_error_network
}
