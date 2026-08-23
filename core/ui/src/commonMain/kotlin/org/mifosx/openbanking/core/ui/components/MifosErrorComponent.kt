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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_no_data
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_no_internet
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_retry
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_something_went_wrong
import template.core.base.designsystem.theme.KptTheme

/**
 * Routes a failure to [NoInternetComponent] or [EmptyDataComponent] based on connectivity.
 *
 * @param message Caller-supplied message; defaults to a generic empty/error string.
 * @param supportReference OBIE support reference surfaced under the message when non-blank.
 */
@Composable
fun MifosErrorComponent(
    modifier: Modifier = Modifier,
    isNetworkConnected: Boolean = true,
    message: String? = null,
    supportReference: String = "",
    isEmptyData: Boolean = false,
    isRetryEnabled: Boolean = false,
    onRetry: () -> Unit = {},
) {
    when {
        !isNetworkConnected -> NoInternetComponent(isRetryEnabled = isRetryEnabled) { onRetry() }
        else -> EmptyDataComponent(
            modifier = modifier,
            isEmptyData = isEmptyData,
            message = message,
            supportReference = supportReference,
            isRetryEnabled = isRetryEnabled,
            onRetry = onRetry,
        )
    }
}

/** Centered offline state with an optional retry button. */
@Composable
fun NoInternetComponent(
    modifier: Modifier = Modifier,
    isRetryEnabled: Boolean = false,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(DesignToken.sizes.avatarMedium),
            imageVector = AppIcons.WifiOff,
            contentDescription = null,
        )

        Text(
            text = stringResource(Res.string.core_ui_no_internet),
            style = KptTheme.typography.titleSmall,
        )

        Spacer(modifier = Modifier.height(KptTheme.spacing.md))

        if (isRetryEnabled) {
            MifosButton(
                modifier = Modifier.wrapContentWidth().align(Alignment.CenterHorizontally),
                onClick = onRetry,
                shape = KptTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(Res.string.core_ui_retry),
                    style = KptTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** Centered empty/error state with an optional retry button. */
@Composable
fun EmptyDataComponent(
    modifier: Modifier = Modifier,
    isEmptyData: Boolean = false,
    message: String? = null,
    supportReference: String = "",
    icon: ImageVector = AppIcons.ErrorCircle,
    isRetryEnabled: Boolean = false,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(KptTheme.spacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(DesignToken.sizes.avatarMedium),
            imageVector = icon,
            contentDescription = null,
        )

        Text(
            text = message ?: if (isEmptyData) {
                stringResource(Res.string.core_ui_no_data)
            } else {
                stringResource(Res.string.core_ui_something_went_wrong)
            },
            style = KptTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )

        if (supportReference.isNotBlank()) {
            Spacer(modifier = Modifier.height(KptTheme.spacing.sm))
            Text(
                text = supportReference,
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(KptTheme.spacing.md))

        if (isRetryEnabled) {
            MifosButton(
                modifier = Modifier.wrapContentWidth().align(Alignment.CenterHorizontally),
                onClick = onRetry,
                shape = KptTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(Res.string.core_ui_retry),
                    style = KptTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun NoInternetPreview() {
    MifosXOpenBankingTheme {
        NoInternetComponent()
    }
}

@Preview
@Composable
private fun EmptyDataPreview() {
    MifosXOpenBankingTheme {
        EmptyDataComponent()
    }
}
