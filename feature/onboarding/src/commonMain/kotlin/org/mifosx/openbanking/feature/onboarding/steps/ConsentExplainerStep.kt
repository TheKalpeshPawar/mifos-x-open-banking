package org.mifosx.openbanking.feature.onboarding.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.onboarding.generated.resources.Res
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_back
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_connect_hsbc
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_how_ob_works
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_legal_footer
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_locked_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_locked_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_password_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_password_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_payments_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_never_payments_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_trust_header
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.onboarding.components.FilledPillButton
import org.mifosx.openbanking.feature.onboarding.components.OutlinedPillButton
import org.mifosx.openbanking.feature.onboarding.components.ReassuranceItem
import org.mifosx.openbanking.feature.onboarding.components.StepIndicator
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingAction
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingUiState
import template.core.base.designsystem.theme.KptTheme

private const val TOTAL_STEPS = 3

@Composable
internal fun ConsentExplainerStep(state: UserOnboardingUiState.ConsentExplainer, onAction: (UserOnboardingAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(KptTheme.spacing.md),
    ) {
        StepIndicator(currentStep = state.step, totalSteps = TOTAL_STEPS)

        Text(stringResource(Res.string.feature_onboarding_trust_header), style = KptTheme.typography.headlineSmall, color = KptTheme.colorScheme.onSurface)
        Spacer(Modifier.height(KptTheme.spacing.md))

        ReassuranceItem(Icons.Outlined.MoneyOff, stringResource(Res.string.feature_onboarding_never_payments_headline), stringResource(Res.string.feature_onboarding_never_payments_body), KptTheme.colorScheme.errorContainer, KptTheme.colorScheme.onErrorContainer)
        ReassuranceItem(Icons.Outlined.Lock, stringResource(Res.string.feature_onboarding_never_password_headline), stringResource(Res.string.feature_onboarding_never_password_body), KptTheme.colorScheme.primaryContainer, KptTheme.colorScheme.onPrimaryContainer)
        ReassuranceItem(Icons.Outlined.Cancel, stringResource(Res.string.feature_onboarding_never_locked_headline), stringResource(Res.string.feature_onboarding_never_locked_body), KptTheme.colorScheme.primaryContainer, KptTheme.colorScheme.onPrimaryContainer)

        Spacer(Modifier.height(KptTheme.spacing.md))
        HorizontalDivider()
        Spacer(Modifier.height(KptTheme.spacing.md))

        Text(stringResource(Res.string.feature_onboarding_legal_footer), style = KptTheme.typography.labelSmall, color = KptTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(KptTheme.spacing.md))

        FilledPillButton(stringResource(Res.string.feature_onboarding_connect_hsbc), onClick = { onAction(UserOnboardingAction.NavigateToLogin) }, icon = Icons.Outlined.OpenInNew, testTag = "onboarding_connect_hsbc")
        Spacer(Modifier.height(KptTheme.spacing.sm))
        OutlinedPillButton(stringResource(Res.string.feature_onboarding_back), onClick = { onAction(UserOnboardingAction.StepBack) }, testTag = "onboarding_step3_back")
        Spacer(Modifier.height(KptTheme.spacing.sm))
        TextButton(
            onClick = { onAction(UserOnboardingAction.OpenObExplainer) },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("onboarding_how_ob_works"),
        ) {
            Text(stringResource(Res.string.feature_onboarding_how_ob_works), style = KptTheme.typography.labelLarge, color = KptTheme.colorScheme.primary)
        }
    }
}
