/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme

/** A confirm/dismiss alert dialog, rendered only while [showDialogState] is true. */
@Composable
fun MifosDialogBox(
    title: String,
    showDialogState: Boolean,
    confirmButtonText: String,
    dismissButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    if (showDialogState) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = { Text(text = title) },
            text = {
                if (message != null) {
                    Text(text = message)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                    },
                ) {
                    Text(text = confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = dismissButtonText)
                }
            },
        )
    }
}

/** A dialog whose content is supplied by the caller. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosCustomDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        content = content,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun MifosDialogBoxPreview() {
    MifosXOpenBankingTheme {
        MifosDialogBox(
            title = "Confirm",
            showDialogState = true,
            confirmButtonText = "Yes",
            dismissButtonText = "No",
            onConfirm = {},
            onDismiss = {},
            message = "Are you sure?",
        )
    }
}
