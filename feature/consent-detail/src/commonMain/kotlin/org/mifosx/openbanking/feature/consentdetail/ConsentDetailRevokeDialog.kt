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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.consentdetail.generated.resources.Res
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_revoke_dialog_a11y
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_revoke_dialog_body
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_revoke_dialog_cancel
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_revoke_dialog_confirm
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_revoke_dialog_title

/**
 * The confirmation gate for revoking access.
 *
 * The destructive choice is the *confirm* button and it carries the error colour, while "Keep
 * access" is the quiet default — a user who dismisses without reading keeps their connection, which
 * is the recoverable outcome. Nothing is issued while this is open; the DELETE only leaves on
 * [onConfirm].
 *
 * Dismissing by tapping outside or pressing back routes to [onDismiss] as well, so there is no way
 * to leave the dialog in a state where the screen is stuck behind a scrim.
 */
@Composable
internal fun ConsentDetailRevokeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_consent_detail_revoke_dialog_a11y)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.feature_consent_detail_revoke_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag(ConsentDetailTestTags.REVOKE_DIALOG_TITLE),
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.feature_consent_detail_revoke_dialog_body),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(ConsentDetailTestTags.REVOKE_DIALOG_BODY),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(ConsentDetailTestTags.REVOKE_DIALOG_CONFIRM),
            ) {
                Text(
                    text = stringResource(Res.string.feature_consent_detail_revoke_dialog_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(ConsentDetailTestTags.REVOKE_DIALOG_CANCEL),
            ) {
                Text(text = stringResource(Res.string.feature_consent_detail_revoke_dialog_cancel))
            }
        },
        modifier = modifier
            .testTag(ConsentDetailTestTags.REVOKE_DIALOG)
            .semantics { contentDescription = description },
    )
}
