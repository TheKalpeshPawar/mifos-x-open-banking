/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.onboarding

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.feature.onboarding.steps.ConsentExplainerStep
import org.mifosx.openbanking.feature.onboarding.steps.EmptyStep
import org.mifosx.openbanking.feature.onboarding.steps.ErrorStep
import org.mifosx.openbanking.feature.onboarding.steps.IntroStep
import org.mifosx.openbanking.feature.onboarding.steps.LoadingStep
import org.mifosx.openbanking.feature.onboarding.steps.ObExplainerOpenStep
import org.mifosx.openbanking.feature.onboarding.steps.PermissionsOverviewStep
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingEvent
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingUiState
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingViewModel
import template.core.base.ui.effects.EventsEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserOnboardingScreen(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserOnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            UserOnboardingEvent.NavigateToLogin -> onNavigateToLogin()
        }
    }

    when (val current = state) {
        is UserOnboardingUiState.Loading -> LoadingStep(modifier)
        is UserOnboardingUiState.Error -> ErrorStep(current, viewModel::trySendAction, modifier)
        is UserOnboardingUiState.Empty -> EmptyStep(viewModel::trySendAction, modifier)
        is UserOnboardingUiState.Intro -> IntroStep(current, viewModel::trySendAction, modifier)
        is UserOnboardingUiState.PermissionsOverview -> PermissionsOverviewStep(
            current,
            viewModel::trySendAction,
            modifier,
        )
        is UserOnboardingUiState.ConsentExplainer -> ConsentExplainerStep(current, viewModel::trySendAction, modifier)
        is UserOnboardingUiState.ObExplainerOpen -> ObExplainerOpenStep(current, viewModel::trySendAction, modifier)
    }
}
