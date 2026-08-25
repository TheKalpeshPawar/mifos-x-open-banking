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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.components.EmptyDataComponent
import org.mifosx.openbanking.core.ui.components.MifosErrorComponent
import org.mifosx.openbanking.core.ui.components.MifosProgressIndicator
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.accountholder.generated.resources.Res
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_empty_title
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_consent_missing_party
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_load_failed
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_profile_not_found
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_error_token_expired
import org.mifosx.openbanking.feature.accountholder.generated.resources.feature_account_holder_screen_title
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderAction
import org.mifosx.openbanking.feature.accountholder.ui.AccountHolderErrorKind
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
        AccountHolderUiState.Loading -> MifosProgressIndicator()

        is AccountHolderUiState.Content -> AccountHolderContent(
            content = current,
            modifier = modifier,
        )

        AccountHolderUiState.Empty -> EmptyDataComponent(
            isEmptyData = true,
            message = stringResource(Res.string.feature_account_holder_empty_title),
        )

        is AccountHolderUiState.Error -> MifosErrorComponent(
            message = stringResource(current.kind.bodyResource()),
            isRetryEnabled = current.kind.isRetriable,
            onRetry = { onAction(AccountHolderAction.RetryLoad) },
        )
    }
}

private fun AccountHolderErrorKind.bodyResource(): StringResource = when (this) {
    AccountHolderErrorKind.TokenExpired ->
        Res.string.feature_account_holder_error_token_expired
    AccountHolderErrorKind.ConsentMissingParty ->
        Res.string.feature_account_holder_error_consent_missing_party
    AccountHolderErrorKind.ProfileNotFound ->
        Res.string.feature_account_holder_error_profile_not_found
    AccountHolderErrorKind.LoadFailed ->
        Res.string.feature_account_holder_error_load_failed
}
