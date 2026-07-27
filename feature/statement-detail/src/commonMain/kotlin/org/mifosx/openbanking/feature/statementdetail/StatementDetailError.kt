/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

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
import org.mifosx.openbanking.feature.statementdetail.generated.resources.Res
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_action_retry
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_a11y
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_consent_missing
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_file_not_found
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_network
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_session_expired
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_statement_not_found
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_title
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailErrorCode

private val IconWellSize = 80.dp
private val IconSize = 40.dp
private val BodyMaxWidth = 280.dp
private val ContentPadding = 24.dp
private val LineGap = 8.dp

/**
 * Error state: a tinted icon well, the failure title, the message for this specific failure code, and a
 * filled Retry button. Every load failure is recoverable — a consent-scope refusal is fixed by
 * re-authorising in Consents and retrying — so Retry always appears.
 */
@Composable
internal fun StatementDetailError(
    code: StatementDetailErrorCode,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_statement_detail_error_a11y)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(StatementDetailTestTags.ERROR_STATE)
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
            text = stringResource(Res.string.feature_statement_detail_error_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(StatementDetailTestTags.ERROR_TITLE),
        )
        Text(
            text = stringResource(code.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(StatementDetailTestTags.ERROR_BODY),
        )
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_statement_detail_action_retry),
            onClick = onRetry,
            testTag = StatementDetailTestTags.RETRY_BUTTON,
        )
    }
}

private fun StatementDetailErrorCode.bodyResource(): StringResource = when (this) {
    StatementDetailErrorCode.StatementNotFound -> Res.string.feature_statement_detail_error_statement_not_found
    StatementDetailErrorCode.ConsentMissingReadStatements ->
        Res.string.feature_statement_detail_error_consent_missing
    StatementDetailErrorCode.SessionExpired -> Res.string.feature_statement_detail_error_session_expired
    StatementDetailErrorCode.StatementFileNotFound -> Res.string.feature_statement_detail_error_file_not_found
    StatementDetailErrorCode.NetworkError -> Res.string.feature_statement_detail_error_network
}
