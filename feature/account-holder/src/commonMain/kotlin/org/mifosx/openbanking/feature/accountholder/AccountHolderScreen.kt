/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.accountholder.generated.resources.Res
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_screen_title
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderAction
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderState
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderUiState
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderViewModel

/**
 * The account holder's identity for a single account, pushed from the account-detail screen.
 *
 * Identity only — consent management and sign-out live in Settings → Consents. Back navigation is
 * delegated upward through [onBack]; the feature never sees the host route table.
 */
@Composable
internal fun AccountHolderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountHolderViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_account_holder_screen_title),
        modifier = modifier,
    ) {
        AccountHolderScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
        )
    }
}

@Composable
internal fun AccountHolderScreenContent(
    state: AccountHolderState,
    onAction: (AccountHolderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        AccountHolderUiState.Loading -> AccountHolderSkeleton(modifier = modifier)

        is AccountHolderUiState.Content -> AccountHolderContent(
            content = current,
            modifier = modifier,
        )

        AccountHolderUiState.Empty -> AccountHolderEmpty(modifier = modifier)

        is AccountHolderUiState.Error -> AccountHolderError(
            kind = current.kind,
            onRetry = { onAction(AccountHolderAction.RetryLoad) },
            modifier = modifier,
        )
    }
}
