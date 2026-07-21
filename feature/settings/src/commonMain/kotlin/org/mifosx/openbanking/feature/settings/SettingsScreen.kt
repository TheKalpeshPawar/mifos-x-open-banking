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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.settings.generated.resources.Res
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_screen_title
import org.mifosx.openbanking.feature.settings.ui.SettingsAction
import org.mifosx.openbanking.feature.settings.ui.SettingsState
import org.mifosx.openbanking.feature.settings.ui.SettingsUiState
import org.mifosx.openbanking.feature.settings.ui.SettingsViewModel

/** The hosted Terms of Service page. */
private const val TERMS_URL = "https://mifos.org/legal/terms-of-service"

/** The hosted Privacy Policy page. */
private const val PRIVACY_URL = "https://mifos.org/legal/privacy-policy"

/** The hosted open-source attribution page. */
private const val LICENCES_URL = "https://mifos.org/legal/open-source-licences"

/**
 * The settings hub. A bottom-nav tab root, so it carries no navigation icon — there is nothing
 * behind it to go back to.
 *
 * Every outbound move is delegated: [onNavigateToProfile] receives the stored account id from
 * state, [onNavigateToConsents] takes none, and the legal pages go out through [onOpenUrl].
 */
@Composable
internal fun SettingsScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToConsents: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = false,
        title = stringResource(Res.string.feature_settings_screen_title),
        modifier = modifier,
    ) {
        SettingsScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToConsents = onNavigateToConsents,
            onOpenUrl = onOpenUrl,
        )
    }
}

/**
 * Renders one of the three states.
 *
 * Separate from [SettingsScreen] and free of the view model so the Compose suites can drive every
 * state directly, without standing up Koin or a preference store.
 */
@Composable
internal fun SettingsScreenContent(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToConsents: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
) {
    when (val current = state.uiState) {
        is SettingsUiState.Content -> SettingsContent(
            themeConfig = current.themeConfig,
            appVersionLabel = current.appVersionLabel,
            isThemeMenuExpanded = current.isThemeMenuExpanded,
            onSelectTheme = { onAction(SettingsAction.SelectTheme(it)) },
            onToggleThemeMenu = { onAction(SettingsAction.ToggleThemeMenu) },
            onDismissThemeMenu = { onAction(SettingsAction.DismissThemeMenu) },
            onNavigateToConsents = onNavigateToConsents,
            onNavigateToProfile = { onNavigateToProfile(current.selectedAccountId) },
            onOpenTerms = { onOpenUrl(TERMS_URL) },
            onOpenPrivacy = { onOpenUrl(PRIVACY_URL) },
            onOpenLicences = { onOpenUrl(LICENCES_URL) },
            modifier = modifier,
        )

        SettingsUiState.Empty -> SettingsEmpty(modifier = modifier)

        is SettingsUiState.Error -> SettingsError(
            kind = current.kind,
            onRetry = { onAction(SettingsAction.RetryLoad) },
            modifier = modifier,
        )
    }
}
