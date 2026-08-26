/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.generated.resources.Res
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_greeting_afternoon
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_greeting_evening
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_greeting_morning
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_hello
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_settings_desc
import org.mifosx.openbanking.feature.home.ui.Greeting
import template.core.base.designsystem.theme.KptTheme

/**
 * The gradient panel at the top of the home screen: the time-of-day greeting and an avatar that opens
 * Settings. The quick-actions card overlaps its lower edge.
 *
 * @param greeting Which time-of-day greeting to show.
 * @param onAvatarClick Invoked when the avatar is tapped.
 */
@Composable
internal fun HomeGradientHeader(
    greeting: Greeting,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DesignToken.sizes.headerPanel)
            .clip(
                KptTheme.shapes.extraLarge.copy(
                    topStart = CornerSize(0.dp),
                    topEnd = CornerSize(0.dp),
                ),
            )
            .background(
                Brush.linearGradient(
                    listOf(KptTheme.colorScheme.primary, KptTheme.colorScheme.primaryContainer),
                ),
            )
            .testTag(HomeTestTags.HEADER),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(KptTheme.spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.feature_home_hello),
                    style = KptTheme.typography.bodyLarge,
                    color = KptTheme.colorScheme.onPrimary,
                )
                Text(
                    text = stringResource(greeting.textRes()),
                    style = KptTheme.typography.headlineLarge,
                    color = KptTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag(HomeTestTags.GREETING),
                )
            }
            Avatar(onClick = onAvatarClick)
        }
    }
}

@Composable
private fun Avatar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(DesignToken.sizes.avatarMedium)
            .clip(DesignToken.shapes.circle)
            .background(KptTheme.colorScheme.primaryContainer)
            .border(DesignToken.strokes.medium, KptTheme.colorScheme.onPrimary, DesignToken.shapes.circle)
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.AVATAR),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Person,
            contentDescription = stringResource(Res.string.feature_home_settings_desc),
            tint = KptTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(DesignToken.sizes.iconMedium),
        )
    }
}

private fun Greeting.textRes() = when (this) {
    Greeting.Morning -> Res.string.feature_home_greeting_morning
    Greeting.Afternoon -> Res.string.feature_home_greeting_afternoon
    Greeting.Evening -> Res.string.feature_home_greeting_evening
}

@Preview
@Composable
private fun HomeGradientHeaderPreview() {
    MifosXOpenBankingTheme {
        HomeGradientHeader(greeting = Greeting.Morning, onAvatarClick = {})
    }
}
