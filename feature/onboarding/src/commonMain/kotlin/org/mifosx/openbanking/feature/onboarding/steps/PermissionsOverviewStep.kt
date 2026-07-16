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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.hsbcPermission.OBPermission
import org.mifosx.openbanking.feature.onboarding.components.FilledPillButton
import org.mifosx.openbanking.feature.onboarding.components.OutlinedPillButton
import org.mifosx.openbanking.feature.onboarding.components.PermissionListItem
import org.mifosx.openbanking.feature.onboarding.components.StepIndicator
import org.mifosx.openbanking.feature.onboarding.generated.resources.Res
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_back
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_consent_duration
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_next
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_permissions_header
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingAction
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingUiState
import template.core.base.designsystem.theme.KptTheme

private const val TOTAL_STEPS = 3

@Composable
internal fun PermissionsOverviewStep(
    state: UserOnboardingUiState.PermissionsOverview,
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
            stringResource(Res.string.feature_onboarding_permissions_header),
            style = KptTheme.typography.headlineSmall,
            color = KptTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(KptTheme.spacing.md))

        OBPermission.ALL.forEach { PermissionListItem(it) }

        Spacer(Modifier.height(KptTheme.spacing.sm))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KptTheme.spacing.sm))
                .background(KptTheme.colorScheme.surfaceContainerLow)
                .padding(KptTheme.spacing.sm),
        ) {
            Icon(
                Icons.Outlined.Schedule,
                null,
                Modifier.size(18.dp),
                tint = KptTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(KptTheme.spacing.sm))
            Text(
                stringResource(Res.string.feature_onboarding_consent_duration),
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(KptTheme.spacing.md))

        FilledPillButton(
            stringResource(Res.string.feature_onboarding_next),
            onClick = { onAction(UserOnboardingAction.StepNext) },
            testTag = "onboarding_step2_next",
        )
        Spacer(Modifier.height(KptTheme.spacing.sm))
        OutlinedPillButton(
            stringResource(Res.string.feature_onboarding_back),
            onClick = { onAction(UserOnboardingAction.StepBack) },
            testTag = "onboarding_step2_back",
        )
    }
}
