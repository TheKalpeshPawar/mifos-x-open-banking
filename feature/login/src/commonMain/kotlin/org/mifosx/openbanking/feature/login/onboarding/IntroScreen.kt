/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.login.generated.resources.Res
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_fapi_chip
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_fca_chip
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_intro_body
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_intro_continue
import org.mifosx.openbanking.feature.login.generated.resources.feature_login_intro_headline
import template.core.base.designsystem.theme.KptTheme
import template.core.base.platform.LocalIntentManager

private val HERO_HEIGHT = 220.dp
private const val FAPI_STANDARD_URL = "https://openid.net/wg/fapi/"
private const val FCA_OPEN_BANKING_URL = "https://www.fca.org.uk/firms/open-banking-fca"

/**
 * The first page of the auth flow: an Open Banking primer with theme-aware hero art and two
 * verifiable trust chips. Self-contained — it knows nothing about the login screen it precedes;
 * [onContinue] is the only outward edge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IntroScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val intentManager = LocalIntentManager.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HERO_HEIGHT)
                .testTag("intro_hero_illustration"),
            contentAlignment = Alignment.Center,
        ) {
            OnboardingHeroIllustration(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
            )
        }

        Spacer(Modifier.height(KptTheme.spacing.md))

        Text(
            stringResource(Res.string.feature_login_intro_headline),
            style = KptTheme.typography.headlineMedium,
            color = KptTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(KptTheme.spacing.md))
        Text(
            stringResource(Res.string.feature_login_intro_body),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(KptTheme.spacing.lg))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            AssistChip(
                Icons.Filled.VerifiedUser,
                stringResource(Res.string.feature_login_fapi_chip),
                onClick = { intentManager.launchUri(FAPI_STANDARD_URL) },
            )
            AssistChip(
                Icons.Filled.AccountBalance,
                stringResource(Res.string.feature_login_fca_chip),
                onClick = { intentManager.launchUri(FCA_OPEN_BANKING_URL) },
            )
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(KptTheme.spacing.md))

        MifosFilledPillButton(
            stringResource(Res.string.feature_login_intro_continue),
            onClick = onContinue,
            testTag = "intro_continue",
        )
    }
}
