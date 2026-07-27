/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ErrorOutline
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
import org.mifosx.openbanking.feature.settings.generated.resources.Res
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_error_accessibility
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_error_preferences_unavailable
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_error_title
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_error_unknown
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_retry
import org.mifosx.openbanking.feature.settings.ui.SettingsErrorKind

private val IconSize = 64.dp
private val BodyMaxWidth = 320.dp
private val ContentPadding = 24.dp
private val LineGap = 12.dp
private val ButtonTopGap = 12.dp
private val ButtonMaxWidth = 220.dp

/**
 * Error state: the preference store could not be read.
 *
 * Retry is rendered whenever the kind is retriable, which both kinds currently are — the check is
 * kept rather than assumed so a future non-retriable kind cannot silently inherit a button that
 * would loop without succeeding.
 */
@Composable
internal fun SettingsError(
    kind: SettingsErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_settings_error_accessibility)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(SettingsTestTags.ERROR_STATE)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(IconSize),
        )
        Text(
            text = stringResource(Res.string.feature_settings_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(SettingsTestTags.ERROR_TITLE),
        )
        Text(
            text = stringResource(kind.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(SettingsTestTags.ERROR_BODY),
        )
        if (kind.isRetriable) {
            MifosFilledPillButton(
                label = stringResource(Res.string.feature_settings_retry),
                onClick = onRetry,
                icon = Icons.Filled.Refresh,
                testTag = SettingsTestTags.RETRY_BUTTON,
                modifier = Modifier
                    .padding(top = ButtonTopGap)
                    .widthIn(max = ButtonMaxWidth),
            )
        }
    }
}

private fun SettingsErrorKind.bodyResource(): StringResource = when (this) {
    SettingsErrorKind.PreferencesUnavailable ->
        Res.string.feature_settings_error_preferences_unavailable

    SettingsErrorKind.Unknown -> Res.string.feature_settings_error_unknown
}
