/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

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
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.Res
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_empty_title
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_error_consent_revoked
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_error_network
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_error_rate_limited
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_error_server
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_error_token_expired
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_screen_title
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesAction
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesErrorKind
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesState
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesUiState
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesViewModel
import org.mifosx.openbanking.feature.beneficiaries.ui.isRetriable

/**
 * Saved-payee list for one account, pushed from the account-detail Beneficiaries chip.
 *
 * Back navigation is delegated upward through [onBack]; the feature never sees the host route
 * table.
 */
@Composable
internal fun BeneficiariesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BeneficiariesViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_beneficiaries_screen_title),
        modifier = modifier,
    ) {
        BeneficiariesScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
        )
    }
}

@Composable
internal fun BeneficiariesScreenContent(
    state: BeneficiariesState,
    onAction: (BeneficiariesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        BeneficiariesUiState.Loading -> MifosProgressIndicator()

        is BeneficiariesUiState.Content -> BeneficiariesContent(
            content = current,
            onSearch = { query -> onAction(BeneficiariesAction.Search(query)) },
            modifier = modifier,
        )

        BeneficiariesUiState.Empty -> EmptyDataComponent(
            isEmptyData = true,
            message = stringResource(Res.string.feature_beneficiaries_empty_title),
        )

        is BeneficiariesUiState.Error -> MifosErrorComponent(
            message = stringResource(current.kind.bodyResource()),
            isRetryEnabled = current.kind.isRetriable,
            onRetry = { onAction(BeneficiariesAction.RetryLoad) },
        )
    }
}

private fun BeneficiariesErrorKind.bodyResource(): StringResource = when (this) {
    BeneficiariesErrorKind.TokenExpired -> Res.string.feature_beneficiaries_error_token_expired
    BeneficiariesErrorKind.ConsentRevoked -> Res.string.feature_beneficiaries_error_consent_revoked
    BeneficiariesErrorKind.RateLimited -> Res.string.feature_beneficiaries_error_rate_limited
    BeneficiariesErrorKind.NetworkError -> Res.string.feature_beneficiaries_error_network
    BeneficiariesErrorKind.ServerError -> Res.string.feature_beneficiaries_error_server
}
