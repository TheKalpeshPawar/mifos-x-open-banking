/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.consentlist.generated.resources.Res
import org.mifosx.openbanking.feature.consentlist.generated.resources.feature_consent_list_screen_title
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListAction
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListState
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListUiState
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListViewModel

/**
 * The PSU's connected banks, pushed from Settings, Profile, Accounts or home.
 *
 * All four outbound routes are delegated upward; the feature never sees the host route table.
 */
@Composable
internal fun ConsentListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onConnectBank: () -> Unit,
    onReauthenticate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConsentListViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_consent_list_screen_title),
        modifier = modifier,
    ) {
        ConsentListScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onNavigateToDetail = onNavigateToDetail,
            onConnectBank = onConnectBank,
            onReauthenticate = onReauthenticate,
        )
    }
}

@Composable
internal fun ConsentListScreenContent(
    state: ConsentListState,
    onAction: (ConsentListAction) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onConnectBank: () -> Unit,
    onReauthenticate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        ConsentListUiState.Loading -> ConsentListSkeleton(modifier = modifier)

        is ConsentListUiState.Content -> ConsentListContent(
            content = current,
            onCardClick = onNavigateToDetail,
            modifier = modifier,
        )

        ConsentListUiState.Empty -> ConsentListEmpty(
            onConnectBank = onConnectBank,
            modifier = modifier,
        )

        is ConsentListUiState.Error -> ConsentListError(
            kind = current.kind,
            onRetry = { onAction(ConsentListAction.RetryLoad) },
            modifier = modifier,
        )

        ConsentListUiState.ErrorAuth -> ConsentListAuthError(
            onReauthenticate = onReauthenticate,
            modifier = modifier,
        )
    }
}
