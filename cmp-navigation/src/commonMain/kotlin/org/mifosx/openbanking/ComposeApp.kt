/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.designsystem.theme.MifosTheme
import org.mifosx.openbanking.rootnav.RootNavScreen
import template.core.base.ui.effects.EventsEffect

@Composable
fun ComposeApp(
    updateScreenCapture: (isScreenCaptureAllowed: Boolean) -> Unit,
    handleRecreate: () -> Unit,
    handleThemeMode: (osValue: Int) -> Unit,
    handleAppLocale: (locale: String?) -> Unit,
    onSplashScreenRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = koinViewModel(),
) {
    val uiState by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isScreenCaptureAllowed) {
        updateScreenCapture(uiState.isScreenCaptureAllowed)
    }

    EventsEffect(eventFlow = viewModel.eventFlow) { event ->
        when (event) {
            is AppEvent.ShowToast -> {}
            is AppEvent.UpdateAppLocale -> handleAppLocale(event.localeName)
            is AppEvent.UpdateAppTheme -> handleThemeMode(event.osValue)
            is AppEvent.Recreate -> handleRecreate()
        }
    }

    val darkTheme = if (uiState.followSystem) isSystemInDarkTheme() else uiState.darkTheme

    MifosTheme(
        darkTheme = darkTheme,
        androidTheme = uiState.isAndroidTheme,
        useDynamicColor = uiState.isDynamicColorsEnabled,
    ) {
        RootNavScreen(
            modifier = modifier,
            onSplashScreenRemoved = onSplashScreenRemoved,
        )
    }
}
