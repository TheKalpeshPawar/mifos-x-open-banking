/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package cmp.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cmp.navigation.rootnav.RootNavScreen
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
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

    val darkTheme = when (uiState.darkThemeConfig) {
        DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        DarkThemeConfig.LIGHT -> false
        DarkThemeConfig.DARK -> true
    }

    MifosXOpenBankingTheme(
        darkTheme = darkTheme,
        androidTheme = uiState.isAndroidTheme,
        useDynamicColor = uiState.isDynamicColorsEnabled,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            RootNavScreen(
                modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                onSplashScreenRemoved = onSplashScreenRemoved,
            )
        }
    }
}
