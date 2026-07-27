/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.account.accountDisplayName
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.accountdetail.generated.resources.Res
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_screen_title
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailAction
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailState
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailUiState
import org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailViewModel
import org.mifosx.openbanking.feature.accountdetail.ui.AccountHeaderUi

/**
 * Account-detail hub. Reached by tapping an account, and the sole entry point to the account's
 * transactions, standing orders, direct debits and the rest of the Explore row.
 *
 * Navigation is delegated upward through [onNavigateToChip] and [onBack]; the feature never sees
 * the host route table.
 */
@Composable
internal fun AccountDetailScreen(
    onNavigateToChip: (chip: AccountDetailChip, accountId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountDetailViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = state.topBarTitle(),
        modifier = modifier,
    ) {
        AccountDetailScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onChipClick = { chip -> onNavigateToChip(chip, state.accountId) },
        )
    }
}

/**
 * The account nickname replaces the generic title once it is known, so the bar reads
 * "Everyday Current" rather than "Account Detail" whenever there is an account to name.
 */
@Composable
private fun AccountDetailState.topBarTitle(): String = when (val current = uiState) {
    is AccountDetailUiState.Content -> current.header.displayName()
    is AccountDetailUiState.Empty -> current.header.displayName()
    AccountDetailUiState.Loading,
    is AccountDetailUiState.Error,
    -> stringResource(Res.string.feature_account_detail_screen_title)
}

@Composable
private fun AccountHeaderUi.displayName(): String =
    accountDisplayName(nickname = nickname, accountSubType = accountSubType, accountNumber = accountNumber)

@Composable
internal fun AccountDetailScreenContent(
    state: AccountDetailState,
    onAction: (AccountDetailAction) -> Unit,
    onChipClick: (AccountDetailChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        AccountDetailUiState.Loading -> AccountDetailSkeleton(modifier = modifier)

        is AccountDetailUiState.Content -> AccountDetailContent(
            header = current.header,
            balances = current.balances,
            availableChips = state.availableChips,
            onChipClick = onChipClick,
            modifier = modifier,
        )

        is AccountDetailUiState.Empty -> AccountDetailContent(
            header = current.header,
            balances = emptyList(),
            availableChips = state.availableChips,
            onChipClick = onChipClick,
            modifier = modifier,
        )

        is AccountDetailUiState.Error -> AccountDetailError(
            kind = current.kind,
            showRetry = current.recoverable,
            onRetry = { onAction(AccountDetailAction.RetryLoad) },
            modifier = modifier,
        )
    }
}
