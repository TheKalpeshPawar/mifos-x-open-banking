package org.mifosx.openbanking.feature.onboarding.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.onboarding.generated.resources.Res
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_fapi_step1_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_fapi_step1_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_fapi_step2_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_fapi_step2_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_fapi_step3_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_fapi_step3_headline
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_got_it
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_ob_explainer_title
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_revoke_note
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.onboarding.components.FilledPillButton
import org.mifosx.openbanking.feature.onboarding.components.OnboardingStep
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingAction
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingUiState
import template.core.base.designsystem.theme.KptTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObExplainerOpenStep(state: UserOnboardingUiState.ObExplainerOpen, onAction: (UserOnboardingAction) -> Unit, modifier: Modifier = Modifier) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Box(modifier = modifier.fillMaxSize()) {
        ConsentExplainerStep(UserOnboardingUiState.ConsentExplainer(step = state.step), onAction = {})

        ModalBottomSheet(
            onDismissRequest = { onAction(UserOnboardingAction.CloseObExplainer) },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = KptTheme.spacing.lg, topEnd = KptTheme.spacing.lg),
            containerColor = KptTheme.colorScheme.surfaceContainerLow,
            dragHandle = {},
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = KptTheme.spacing.lg).padding(bottom = 32.dp)) {
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally).width(32.dp).height(4.dp)
                        .clip(RoundedCornerShape(50)).background(KptTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.height(KptTheme.spacing.md))
                Text(stringResource(Res.string.feature_onboarding_ob_explainer_title), style = KptTheme.typography.titleLarge, color = KptTheme.colorScheme.onSurface)
                Spacer(Modifier.height(KptTheme.spacing.lg))
                OnboardingStep(Icons.Rounded.HowToReg, stringResource(Res.string.feature_onboarding_fapi_step1_headline), stringResource(Res.string.feature_onboarding_fapi_step1_body))
                OnboardingStep(Icons.Rounded.Login, stringResource(Res.string.feature_onboarding_fapi_step2_headline), stringResource(Res.string.feature_onboarding_fapi_step2_body))
                OnboardingStep(Icons.Outlined.Shield, stringResource(Res.string.feature_onboarding_fapi_step3_headline), stringResource(Res.string.feature_onboarding_fapi_step3_body))
                Spacer(Modifier.height(KptTheme.spacing.md))
                Text(
                    stringResource(Res.string.feature_onboarding_revoke_note),
                    style = KptTheme.typography.bodySmall, color = KptTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(KptTheme.spacing.sm)).background(KptTheme.colorScheme.surfaceContainer).padding(KptTheme.spacing.sm),
                )
                Spacer(Modifier.height(KptTheme.spacing.lg))
                FilledPillButton(stringResource(Res.string.feature_onboarding_got_it), onClick = { onAction(UserOnboardingAction.CloseObExplainer) }, testTag = "onboarding_ob_explainer_got_it")
            }
        }
    }
}
