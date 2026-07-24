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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.Res
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_screen_title
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesAction
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesState
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesUiState
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesViewModel

/**
 * Saved-payee list for one account, pushed from the account-detail Beneficiaries chip.
 *
 * Back navigation and the revoked-consent recovery route are delegated upward through [onBack] and
 * [onNavigateToConsents]; the feature never sees the host route table.
 */
@Composable
internal fun BeneficiariesScreen(
    onBack: () -> Unit,
    onNavigateToConsents: () -> Unit,
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
            onNavigateToConsents = onNavigateToConsents,
        )
    }
}

@Composable
internal fun BeneficiariesScreenContent(
    state: BeneficiariesState,
    onAction: (BeneficiariesAction) -> Unit,
    onNavigateToConsents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        BeneficiariesUiState.Loading -> BeneficiariesSkeleton(modifier = modifier)

        is BeneficiariesUiState.Content -> BeneficiariesContent(
            content = current,
            onSearch = { query -> onAction(BeneficiariesAction.Search(query)) },
            modifier = modifier,
        )

        BeneficiariesUiState.Empty -> BeneficiariesEmpty(modifier = modifier)

        is BeneficiariesUiState.Error -> BeneficiariesError(
            kind = current.kind,
            onRetry = { onAction(BeneficiariesAction.RetryLoad) },
            onViewConsents = onNavigateToConsents,
            modifier = modifier,
        )
    }
}
