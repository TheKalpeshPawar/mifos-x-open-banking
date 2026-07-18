/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.login.components.PermissionList
import org.mifosx.openbanking.feature.login.generated.resources.Res
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_authorising_hint
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_cancel
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_connecting
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_consent_validity
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_continue_hsbc
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_empty_body
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_empty_title
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_error_title
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_explainer_body
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_explainer_headline
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_go_back
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_ob_badge
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_opening_hsbc
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_permissions_header
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_retry
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_security_notice
import org.mifosx.openbanking.feature.login.generated.resources.hsbc_logo
import org.mifosx.openbanking.feature.login.ui.LoginAction
import org.mifosx.openbanking.feature.login.ui.LoginEvent
import org.mifosx.openbanking.feature.login.ui.LoginUiState
import org.mifosx.openbanking.feature.login.ui.LoginViewModel
import template.core.base.designsystem.theme.KptTheme
import template.core.base.platform.LocalIntentManager
import template.core.base.ui.effects.EventsEffect

@Composable
internal fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val intentManager = LocalIntentManager.current

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            LoginEvent.NavigateBack -> intentManager.exitApplication()
        }
    }

    when (val current = state) {
        is LoginUiState.Loading -> LoadingState(modifier)
        is LoginUiState.Content -> ContentState(current, viewModel::trySendAction, modifier)
        is LoginUiState.Authorising -> AuthorisingState(modifier)
        is LoginUiState.Error -> ErrorState(current, viewModel::trySendAction, modifier)
        is LoginUiState.Empty -> EmptyState(viewModel::trySendAction, modifier)
    }
}

@Composable
internal fun LoadingState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag(LoginTestTags.LOADING_PROGRESS))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.feature_login_connecting),
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ContentState(
    state: LoginUiState.Content,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.md),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag(LoginTestTags.EXPLAINER_CARD),
            shape = RoundedCornerShape(KptTheme.spacing.sm),
            colors = CardDefaults.cardColors(
                containerColor = KptTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(modifier = Modifier.padding(KptTheme.spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(Res.drawable.hsbc_logo),
                        contentDescription = "HSBC",
                        modifier = Modifier.height(20.dp),
                    )
                    Spacer(Modifier.width(KptTheme.spacing.sm))
                    Text(
                        "HSBC",
                        style = KptTheme.typography.titleMedium,
                        color = KptTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(KptTheme.spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(16.dp), tint = KptTheme.colorScheme.primary)
                    Text(
                        " ${stringResource(Res.string.feature_login_ob_badge)}",
                        style = KptTheme.typography.labelSmall,
                        color = KptTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(KptTheme.spacing.sm))
                Text(
                    stringResource(Res.string.feature_login_explainer_headline),
                    style = KptTheme.typography.titleMedium,
                    color = KptTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(KptTheme.spacing.sm))
                Text(
                    stringResource(Res.string.feature_login_explainer_body),
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(KptTheme.spacing.sm))
                HorizontalDivider()
                Spacer(Modifier.height(KptTheme.spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(14.dp), tint = KptTheme.colorScheme.onSurfaceVariant)
                    Text(
                        " ${stringResource(Res.string.feature_login_security_notice)}",
                        style = KptTheme.typography.labelSmall,
                        color = KptTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(KptTheme.spacing.md))

        Text(
            stringResource(Res.string.feature_login_permissions_header),
            style = KptTheme.typography.titleSmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = KptTheme.spacing.sm),
        )
        PermissionList(state.permissions)

        Spacer(Modifier.height(KptTheme.spacing.sm))
        Text(
            stringResource(Res.string.feature_login_consent_validity),
            style = KptTheme.typography.bodySmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = KptTheme.spacing.md),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            state.expiryLabel,
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = KptTheme.spacing.md),
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(KptTheme.spacing.md))

        MifosFilledPillButton(
            stringResource(Res.string.feature_login_continue_hsbc),
            onClick = { onAction(LoginAction.StartOAuth) },
            icon = Icons.Outlined.OpenInNew,
            testTag = LoginTestTags.CONTINUE_HSBC,
        )
        Spacer(Modifier.height(KptTheme.spacing.sm))
        TextButton(
            onClick = { onAction(LoginAction.Cancel) },
            modifier = Modifier.fillMaxWidth().testTag(LoginTestTags.CANCEL),
        ) {
            Text(
                stringResource(Res.string.feature_login_cancel),
                style = KptTheme.typography.labelLarge,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AuthorisingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().testTag(LoginTestTags.AUTHORISING), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp).testTag(LoginTestTags.AUTHORISING_SPINNER))
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(Res.string.feature_login_opening_hsbc),
                style = KptTheme.typography.bodyLarge,
                color = KptTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(KptTheme.spacing.sm))
            Text(
                stringResource(Res.string.feature_login_authorising_hint),
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 260.dp),
            )
        }
    }
}

@Composable
internal fun ErrorState(
    state: LoginUiState.Error,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().testTag(LoginTestTags.ERROR), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(KptTheme.spacing.lg)) {
            Icon(Icons.Outlined.Lock, null, Modifier.size(64.dp), tint = KptTheme.colorScheme.error)
            Spacer(Modifier.height(KptTheme.spacing.lg))
            Text(
                stringResource(Res.string.feature_login_error_title),
                style = KptTheme.typography.headlineSmall,
                color = KptTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(KptTheme.spacing.sm))
            Text(
                state.message,
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
            Spacer(Modifier.height(KptTheme.spacing.lg))
            MifosFilledPillButton(
                stringResource(Res.string.feature_login_retry),
                onClick = { onAction(LoginAction.Retry) },
                testTag = LoginTestTags.ERROR_RETRY,
            )
        }
    }
}

@Composable
internal fun EmptyState(
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().testTag(LoginTestTags.EMPTY), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(KptTheme.spacing.lg)) {
            Icon(Icons.Outlined.ManageSearch, null, Modifier.size(64.dp), tint = KptTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(KptTheme.spacing.lg))
            Text(
                stringResource(Res.string.feature_login_empty_title),
                style = KptTheme.typography.headlineSmall,
                color = KptTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(KptTheme.spacing.sm))
            Text(
                stringResource(Res.string.feature_login_empty_body),
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
            Spacer(Modifier.height(KptTheme.spacing.lg))
            MifosFilledPillButton(
                stringResource(Res.string.feature_login_go_back),
                onClick = { onAction(LoginAction.Cancel) },
                testTag = LoginTestTags.EMPTY_GO_BACK,
            )
        }
    }
}
