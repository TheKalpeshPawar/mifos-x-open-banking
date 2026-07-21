/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.profile.ProfileTestTags
import org.mifosx.openbanking.feature.profile.generated.resources.Res
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_button_cancel
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_button_sign_out
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_sign_out_dialog_body
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_sign_out_dialog_title

/**
 * Confirmation for signing out.
 *
 * The body states the consequence rather than restating the action: clearing the consent means
 * re-authorising with HSBC, which is the part a user cannot undo and would not otherwise expect.
 *
 * The confirm button is filled with the error colour while Cancel stays a plain text button — the
 * destructive choice is the one being confirmed, so it carries the weight, and a dismissal gesture
 * outside the dialog resolves to Cancel.
 */
@Composable
internal fun ProfileSignOutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(ProfileTestTags.SIGN_OUT_DIALOG),
        title = {
            Text(
                text = stringResource(Res.string.feature_profile_sign_out_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.feature_profile_sign_out_dialog_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.testTag(ProfileTestTags.SIGN_OUT_CONFIRM_BUTTON),
            ) {
                Text(
                    text = stringResource(Res.string.feature_profile_button_sign_out),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(ProfileTestTags.SIGN_OUT_CANCEL_BUTTON),
            ) {
                Text(
                    text = stringResource(Res.string.feature_profile_button_cancel),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}
