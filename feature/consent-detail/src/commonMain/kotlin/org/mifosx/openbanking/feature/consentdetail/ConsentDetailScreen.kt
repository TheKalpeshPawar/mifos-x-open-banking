/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.consentdetail.generated.resources.Res
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_screen_title
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailAction
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailState
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailUiState
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailViewModel

/**
 * One consent in full, pushed from a consent-list card.
 *
 * Revoking a consent is a full logout: it tears down the session and the root navigator moves the
 * user to onboarding on its own, so this screen raises no navigation of its own for that path.
 */
@Composable
internal fun ConsentDetailScreen(
    onBack: () -> Unit,
    onReconfirm: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConsentDetailViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = state.uiState !is ConsentDetailUiState.Revoking,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_consent_detail_screen_title),
        modifier = modifier,
    ) {
        ConsentDetailScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onReconfirm = onReconfirm,
            onGoBack = onBack,
        )
    }
}

@Composable
internal fun ConsentDetailScreenContent(
    state: ConsentDetailState,
    onAction: (ConsentDetailAction) -> Unit,
    onReconfirm: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        ConsentDetailUiState.Loading -> ConsentDetailSkeleton(modifier = modifier)

        is ConsentDetailUiState.Content -> ConsentDetailContent(
            consent = current.consent,
            onReconfirm = onReconfirm,
            onRevokeClick = { onAction(ConsentDetailAction.ConfirmRevoke) },
            modifier = modifier,
        )

        is ConsentDetailUiState.RevokeConfirm -> {
            // The content stays rendered behind the gate, with its actions inert, so cancelling
            // returns the user to exactly the screen they were looking at.
            ConsentDetailContent(
                consent = current.consent,
                onReconfirm = onReconfirm,
                onRevokeClick = {},
                actionsEnabled = false,
                modifier = modifier,
            )
            ConsentDetailRevokeDialog(
                onConfirm = { onAction(ConsentDetailAction.ExecuteRevoke) },
                onDismiss = { onAction(ConsentDetailAction.DismissRevokeConfirm) },
            )
        }

        is ConsentDetailUiState.Revoking -> ConsentDetailRevoking(modifier = modifier)

        ConsentDetailUiState.Empty -> ConsentDetailEmpty(onGoBack = onGoBack, modifier = modifier)

        is ConsentDetailUiState.Error -> ConsentDetailError(
            kind = current.kind,
            onRetry = { onAction(ConsentDetailAction.RetryLoad) },
            modifier = modifier,
        )
    }
}
