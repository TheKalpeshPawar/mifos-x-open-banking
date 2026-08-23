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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_loading
import template.core.base.designsystem.theme.KptTheme

/** A modal loading dialog with a spinner, non-dismissible by back or outside click. */
@Composable
fun MifosLoadingDialog(
    visibilityState: LoadingDialogState,
    modifier: Modifier = Modifier,
) {
    when (visibilityState) {
        is LoadingDialogState.Hidden -> Unit
        is LoadingDialogState.Shown -> {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
            ) {
                Card(
                    shape = KptTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = KptTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = modifier
                        .semantics {
                            testTag = "AlertPopup"
                        }
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(Res.string.core_ui_loading),
                            modifier = Modifier
                                .testTag("AlertTitleText")
                                .padding(
                                    top = KptTheme.spacing.lg,
                                    bottom = KptTheme.spacing.sm,
                                ),
                        )
                        CircularProgressIndicator(
                            modifier = Modifier
                                .testTag("AlertProgressIndicator")
                                .padding(
                                    top = KptTheme.spacing.sm,
                                    bottom = KptTheme.spacing.lg,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MifosLoadingDialogPreview() {
    MifosXOpenBankingTheme {
        MifosLoadingDialog(
            visibilityState = LoadingDialogState.Shown,
        )
    }
}

/** Models display of a [MifosLoadingDialog]. */
sealed class LoadingDialogState {
    data object Hidden : LoadingDialogState()

    data object Shown : LoadingDialogState()
}
