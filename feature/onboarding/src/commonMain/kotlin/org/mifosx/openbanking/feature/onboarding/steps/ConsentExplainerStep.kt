/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.onboarding.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.onboarding.components.FilledPillButton
import org.mifosx.openbanking.feature.onboarding.components.OutlinedPillButton
import org.mifosx.openbanking.feature.onboarding.components.ReassuranceItem
import org.mifosx.openbanking.feature.onboarding.components.StepIndicator
import org.mifosx.openbanking.feature.onboarding.generated.resources.Res
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_back
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_connect_hsbc
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_locked_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_locked_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_password_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_password_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_payments_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_payments_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_trust_header
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingAction
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingUiState
import template.core.base.designsystem.theme.KptTheme

private const val TOTAL_STEPS = 2

@Composable
internal fun ConsentExplainerStep(
    state: UserOnboardingUiState.ConsentExplainer,
    onAction: (UserOnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.md),
    ) {
        StepIndicator(currentStep = state.step, totalSteps = TOTAL_STEPS)

        Text(
            stringResource(Res.string.feature_onboarding_trust_header),
            style = KptTheme.typography.headlineSmall,
            color = KptTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(KptTheme.spacing.md))

        ReassuranceItem(
            Icons.Outlined.MoneyOff,
            stringResource(Res.string.feature_onboarding_never_payments_headline),
            stringResource(Res.string.feature_onboarding_never_payments_body),
            KptTheme.colorScheme.errorContainer,
            KptTheme.colorScheme.onErrorContainer,
        )
        ReassuranceItem(
            Icons.Outlined.Lock,
            stringResource(Res.string.feature_onboarding_never_password_headline),
            stringResource(Res.string.feature_onboarding_never_password_body),
            KptTheme.colorScheme.primaryContainer,
            KptTheme.colorScheme.onPrimaryContainer,
        )
        ReassuranceItem(
            Icons.Outlined.Cancel,
            stringResource(Res.string.feature_onboarding_never_locked_headline),
            stringResource(Res.string.feature_onboarding_never_locked_body),
            KptTheme.colorScheme.primaryContainer,
            KptTheme.colorScheme.onPrimaryContainer,
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(KptTheme.spacing.md))

        FilledPillButton(
            stringResource(Res.string.feature_onboarding_connect_hsbc),
            onClick = { onAction(UserOnboardingAction.NavigateToLogin) },
            icon = Icons.Outlined.OpenInNew,
            testTag = "onboarding_connect_hsbc",
        )
        Spacer(Modifier.height(KptTheme.spacing.sm))
        OutlinedPillButton(
            stringResource(Res.string.feature_onboarding_back),
            onClick = { onAction(UserOnboardingAction.StepBack) },
            testTag = "onboarding_step3_back",
        )
    }
}
