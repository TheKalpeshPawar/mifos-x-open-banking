/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.utils.LottieConstants
import template.core.base.designsystem.theme.KptTheme

/** Full-screen looping Lottie loading indicator. */
@Composable
fun MifosProgressIndicator(
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes(LottieConstants.LOADING_ANIMATION).decodeToString(),
        )
    }

    val progress by animateLottieCompositionAsState(
        composition,
        iterations = Int.MAX_VALUE,
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                progress = { progress },
            ),
            contentDescription = null,
        )
    }
}

/** Non-interactive loading overlay over a dimmed background. */
@Composable
fun MifosProgressIndicatorOverlay(
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes(LottieConstants.LOADING_ANIMATION).decodeToString(),
        )
    }

    val progress by animateLottieCompositionAsState(
        composition,
        iterations = Int.MAX_VALUE,
    )

    Box(
        modifier = modifier
            .background(KptTheme.colorScheme.background.copy(alpha = 0.7f))
            .clickable(
                enabled = false,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                progress = { progress },
            ),
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun LoadingPreview() {
    MifosXOpenBankingTheme {
        MifosProgressIndicator()
    }
}

@Preview
@Composable
private fun OverlayLoadingPreview() {
    MifosXOpenBankingTheme {
        MifosProgressIndicatorOverlay()
    }
}
