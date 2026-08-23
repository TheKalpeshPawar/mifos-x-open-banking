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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/**
 * A modal bottom sheet dismissed through its own hide animation. The sibling also registered an
 * Essenty [BackCallback], which is dropped here — this project uses Compose Navigation, and
 * [ModalBottomSheet] already routes the back gesture through [onDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(true) }

    fun dismissSheet() {
        coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
            if (!modalSheetState.isVisible) {
                showBottomSheet = false
            }
        }
        onDismiss.invoke()
    }

    AnimatedVisibility(visible = showBottomSheet) {
        ModalBottomSheet(
            containerColor = KptTheme.colorScheme.surface,
            onDismissRequest = {
                showBottomSheet = false
                dismissSheet()
            },
            sheetState = modalSheetState,
            modifier = modifier,
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun MifosBottomSheetPreview() {
    MifosXOpenBankingTheme {
        MifosBottomSheet(
            content = {
                Box(Modifier.padding(KptTheme.spacing.lg)) {
                    Text("Sheet content")
                }
            },
            onDismiss = {},
        )
    }
}
