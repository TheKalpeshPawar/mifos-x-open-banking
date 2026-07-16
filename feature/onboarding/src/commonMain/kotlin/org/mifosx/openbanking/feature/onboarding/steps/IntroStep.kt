package org.mifosx.openbanking.feature.onboarding.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.onboarding.generated.resources.Res
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_fapi_chip
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_fca_chip
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_intro_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_intro_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_next
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.onboarding.components.AssistChip
import org.mifosx.openbanking.feature.onboarding.components.FilledPillButton
import org.mifosx.openbanking.feature.onboarding.components.StepIndicator
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingAction
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingUiState
import template.core.base.designsystem.theme.KptTheme

private const val TOTAL_STEPS = 3

@Composable
internal fun IntroStep(state: UserOnboardingUiState.Intro, onAction: (UserOnboardingAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(KptTheme.spacing.md),
    ) {
        StepIndicator(currentStep = state.step, totalSteps = TOTAL_STEPS)

        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).padding(KptTheme.spacing.lg)
                .clip(RoundedCornerShape(KptTheme.spacing.sm))
                .background(KptTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                .testTag("onboarding_hero_illustration"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Shield, "Open Banking secure connection illustration", Modifier.size(80.dp), tint = KptTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(KptTheme.spacing.md))

        Text(stringResource(Res.string.feature_onboarding_intro_headline), style = KptTheme.typography.headlineMedium, color = KptTheme.colorScheme.onSurface)
        Spacer(Modifier.height(KptTheme.spacing.md))
        Text(stringResource(Res.string.feature_onboarding_intro_body), style = KptTheme.typography.bodyMedium, color = KptTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
            AssistChip(Icons.Filled.VerifiedUser, stringResource(Res.string.feature_onboarding_fapi_chip))
            AssistChip(Icons.Filled.AccountBalance, stringResource(Res.string.feature_onboarding_fca_chip))
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(KptTheme.spacing.md))

        FilledPillButton(stringResource(Res.string.feature_onboarding_next), onClick = { onAction(UserOnboardingAction.StepNext) }, testTag = "onboarding_step1_next")
    }
}
