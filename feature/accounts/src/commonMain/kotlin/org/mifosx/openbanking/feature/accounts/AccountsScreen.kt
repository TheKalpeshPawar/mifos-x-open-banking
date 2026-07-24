/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.accounts.generated.resources.Res
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_screen_title
import org.mifosx.openbanking.feature.accounts.ui.AccountsAction
import org.mifosx.openbanking.feature.accounts.ui.AccountsViewModel
import template.core.base.ui.screen.ScreenContent

/**
 * Accounts overview: every consented account with its balance, a net-GBP total, and a type filter.
 * Renders across loading / content / empty / error states inside the app scaffold; navigation is
 * delegated to the caller so the feature stays decoupled from the app's route table.
 */
@Composable
internal fun AccountsScreen(
    onNavigateToAccountDetail: (accountId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = false,
        modifier = modifier,
        title = stringResource(Res.string.feature_accounts_screen_title),
    ) {
        ScreenContent(
            state = state.uiState,
            onRetry = { viewModel.trySendAction(AccountsAction.RetryLoad) },
            loading = { AccountsSkeleton() },
            empty = { AccountsEmpty() },
            error = { AccountsError(onRetry = { viewModel.trySendAction(AccountsAction.RetryLoad) }) },
        ) { data, _ ->
            AccountsContent(
                data = data,
                onFilterChange = { viewModel.trySendAction(AccountsAction.FilterAccounts(it)) },
                onAccountClick = { accountId -> onNavigateToAccountDetail(accountId) },
            )
        }
    }
}
