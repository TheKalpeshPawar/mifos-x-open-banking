package org.mifosx.openbanking.feature.onboarding.steps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.onboarding.generated.resources.Res
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_empty_body
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_empty_skip
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_empty_title
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.onboarding.components.FilledPillButton
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingAction
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun EmptyStep(onAction: (UserOnboardingAction) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().testTag("onboarding_empty_state"), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(KptTheme.spacing.lg)) {
            Icon(Icons.Outlined.Info, null, Modifier.size(64.dp), tint = KptTheme.colorScheme.primary)
            Spacer(Modifier.height(KptTheme.spacing.md))
            Text(stringResource(Res.string.feature_onboarding_empty_title), style = KptTheme.typography.titleMedium, color = KptTheme.colorScheme.onSurface)
            Spacer(Modifier.height(KptTheme.spacing.md))
            Text(
                stringResource(Res.string.feature_onboarding_empty_body),
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
            Spacer(Modifier.height(KptTheme.spacing.lg))
            FilledPillButton(
                label = stringResource(Res.string.feature_onboarding_empty_skip),
                onClick = { onAction(UserOnboardingAction.NavigateToLogin) },
                testTag = "onboarding_empty_skip_button",
            )
        }
    }
}
