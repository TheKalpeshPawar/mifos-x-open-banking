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
import org.mifosx.openbanking.feature.accountholder.generated.resources.Res
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_button_retry
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_accessibility
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_consent_missing_party
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_load_failed
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_profile_not_found
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_title
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_token_expired
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderErrorKind

private val IconSize = 48.dp
private val BodyMaxWidth = 320.dp
private val ContentPadding = 24.dp
private val LineGap = 16.dp

/**
 * Error state: the error glyph, the failure title, the message for this specific failure, and a
 * Retry button when retrying can help.
 *
 * Retry appears only when retrying can actually help. A consent that never carried `ReadParty`, and
 * a bank with no identity record for the account, both fail identically on every attempt — so the
 * button is withheld there rather than inviting a loop that cannot succeed.
 */
@Composable
internal fun AccountHolderError(
    kind: AccountHolderErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_account_holder_error_accessibility)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(AccountHolderTestTags.ERROR_STATE)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(IconSize),
        )
        Text(
            text = stringResource(Res.string.feature_account_holder_error_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(AccountHolderTestTags.ERROR_TITLE),
        )
        Text(
            text = stringResource(kind.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(AccountHolderTestTags.ERROR_BODY),
        )
        if (kind.isRetriable) {
            MifosFilledPillButton(
                label = stringResource(Res.string.feature_account_holder_button_retry),
                onClick = onRetry,
                testTag = AccountHolderTestTags.RETRY_BUTTON,
            )
        }
    }
}

private fun AccountHolderErrorKind.bodyResource(): StringResource = when (this) {
    AccountHolderErrorKind.TokenExpired -> Res.string.feature_account_holder_error_token_expired
    AccountHolderErrorKind.ConsentMissingParty -> Res.string.feature_account_holder_error_consent_missing_party
    AccountHolderErrorKind.ProfileNotFound -> Res.string.feature_account_holder_error_profile_not_found
    AccountHolderErrorKind.LoadFailed -> Res.string.feature_account_holder_error_load_failed
}
