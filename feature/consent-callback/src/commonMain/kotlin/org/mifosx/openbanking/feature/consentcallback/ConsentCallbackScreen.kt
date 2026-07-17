/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentcallback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.feature.consentcallback.steps.AccessDeniedStep
import org.mifosx.openbanking.feature.consentcallback.steps.AwaitingStep
import org.mifosx.openbanking.feature.consentcallback.steps.CallbackErrorStep
import org.mifosx.openbanking.feature.consentcallback.steps.LoadingStep
import org.mifosx.openbanking.feature.consentcallback.steps.SecurityErrorStep
import org.mifosx.openbanking.feature.consentcallback.steps.SuccessStep
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackAction
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackEvent
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackUiState
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackViewModel
import template.core.base.ui.effects.EventsEffect

@Composable
internal fun ConsentCallbackScreen(
    redirectUrl: String,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConsentCallbackViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    /**
     * Keyed on [redirectUrl] rather than `Unit` so a recomposition cannot re-submit the callback:
     * validation consumes the single-use PendingAuth, so a second dispatch would fail as a replay.
     */
    LaunchedEffect(redirectUrl) {
        viewModel.trySendAction(ConsentCallbackAction.ProcessCallback(redirectUrl))
    }

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            ConsentCallbackEvent.NavigateToHome -> onNavigateToHome()
            ConsentCallbackEvent.NavigateToLogin -> onNavigateToLogin()
        }
    }

    when (state) {
        is ConsentCallbackUiState.Loading -> LoadingStep(modifier)
        is ConsentCallbackUiState.Content -> SuccessStep(modifier)
        is ConsentCallbackUiState.Awaiting -> AwaitingStep(viewModel::trySendAction, modifier)
        is ConsentCallbackUiState.Error -> CallbackErrorStep(viewModel::trySendAction, modifier)
        is ConsentCallbackUiState.AccessDenied -> AccessDeniedStep(viewModel::trySendAction, modifier)
        is ConsentCallbackUiState.SecurityError -> SecurityErrorStep(viewModel::trySendAction, modifier)
    }
}
