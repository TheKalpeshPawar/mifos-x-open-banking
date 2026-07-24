/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.profile.components.ProfileSignOutDialog
import org.mifosx.openbanking.feature.profile.generated.resources.Res
import org.mifosx.openbanking.feature.profile.generated.resources.feature_profile_screen_title
import org.mifosx.openbanking.feature.profile.ui.ProfileAction
import org.mifosx.openbanking.feature.profile.ui.ProfileEvent
import org.mifosx.openbanking.feature.profile.ui.ProfileState
import org.mifosx.openbanking.feature.profile.ui.ProfileUiState
import org.mifosx.openbanking.feature.profile.ui.ProfileViewModel

/**
 * The connected party's identity and the Open Banking consent behind it, pushed from the More tab.
 *
 * Back navigation is delegated upward through [onBack] and the consent destination through
 * [onNavigateToConsents]; the feature never sees the host route table.
 */
@Composable
internal fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToConsents: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                ProfileEvent.NavigateToConsents -> onNavigateToConsents()
            }
        }
    }

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_profile_screen_title),
        modifier = modifier,
    ) {
        ProfileScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
        )
    }
}

@Composable
internal fun ProfileScreenContent(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        ProfileUiState.Loading -> ProfileSkeleton(modifier = modifier)

        is ProfileUiState.Content -> ProfileContent(
            content = current,
            onAction = onAction,
            modifier = modifier,
        )

        ProfileUiState.Empty -> ProfileEmpty(
            onReauthorise = { onAction(ProfileAction.Reauthorise) },
            modifier = modifier,
        )

        is ProfileUiState.Error -> ProfileError(
            kind = current.kind,
            onRetry = { onAction(ProfileAction.RetryLoad) },
            onSignOut = { onAction(ProfileAction.RequestSignOut) },
            modifier = modifier,
        )
    }

    // The confirmation is screen-level, so it renders over whatever state is showing — including the
    // error state an expired consent produces, where sign-out is the only way forward.
    if (state.isConfirmingSignOut) {
        ProfileSignOutDialog(
            onConfirm = { onAction(ProfileAction.ConfirmSignOut) },
            onDismiss = { onAction(ProfileAction.DismissSignOut) },
        )
    }
}
