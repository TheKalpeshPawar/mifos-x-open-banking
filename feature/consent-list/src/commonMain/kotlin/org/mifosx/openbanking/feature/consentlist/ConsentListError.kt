/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentlist

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
import androidx.compose.material.icons.outlined.LockOpen
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.consentlist.generated.resources.Res
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_auth_error_a11y
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_auth_error_body
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_auth_error_title
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_error_a11y
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_error_network
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_error_server
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_error_title
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_reauth
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_retry
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListErrorKind

private val ErrorIconSize = 64.dp
private val AuthIconWellSize = 80.dp
private val AuthIconSize = 40.dp
private val BodyMaxWidth = 300.dp
private val ContentPadding = 24.dp
private val LineGap = 16.dp
private const val ICON_ALPHA = 0.8f

/**
 * Generic error state: a dimmed error glyph, the failure title, the message for this failure, and
 * Retry.
 *
 * Both kinds here are transient, so both offer Retry. An expired session is not one of them — it
 * renders [ConsentListAuthError] instead.
 */
@Composable
internal fun ConsentListError(
    kind: ConsentListErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_consent_list_error_a11y)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(ConsentListTestTags.ERROR_STATE)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(ErrorIconSize).alpha(ICON_ALPHA),
        )
        Text(
            text = stringResource(Res.string.feature_consent_list_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(ConsentListTestTags.ERROR_TITLE),
        )
        Text(
            text = stringResource(kind.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(ConsentListTestTags.ERROR_BODY),
        )
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_consent_list_retry),
            onClick = onRetry,
            testTag = ConsentListTestTags.RETRY_BUTTON,
        )
    }
}

/**
 * Expired-session state.
 *
 * Deliberately tertiary rather than error red, and a filled well rather than a bare glyph: the
 * mockup's own note is that this needs action but is not a failure. It offers no Retry — retrying an
 * expired session just fails again, so the only route out is signing in.
 */
@Composable
internal fun ConsentListAuthError(
    onReauthenticate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_consent_list_auth_error_a11y)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(ConsentListTestTags.AUTH_ERROR_STATE)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(AuthIconWellSize)
                .background(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.LockOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(AuthIconSize),
            )
        }
        Text(
            text = stringResource(Res.string.feature_consent_list_auth_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(ConsentListTestTags.AUTH_ERROR_TITLE),
        )
        Text(
            text = stringResource(Res.string.feature_consent_list_auth_error_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(ConsentListTestTags.AUTH_ERROR_BODY),
        )
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_consent_list_reauth),
            onClick = onReauthenticate,
            testTag = ConsentListTestTags.REAUTH_BUTTON,
        )
    }
}

private fun ConsentListErrorKind.bodyResource(): StringResource = when (this) {
    ConsentListErrorKind.NetworkError -> Res.string.feature_consent_list_error_network
    ConsentListErrorKind.ServerError -> Res.string.feature_consent_list_error_server
}
