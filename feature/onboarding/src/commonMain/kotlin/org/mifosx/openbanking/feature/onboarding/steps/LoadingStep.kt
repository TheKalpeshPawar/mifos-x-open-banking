package org.mifosx.openbanking.feature.onboarding.steps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.onboarding.generated.resources.Res
import org.mifosx.openbanking.feature.onboarding.generated.resources.feature_onboarding_loading
import org.jetbrains.compose.resources.stringResource
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun LoadingStep(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().testTag("onboarding_loading_spinner"), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(Res.string.feature_onboarding_loading),
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
