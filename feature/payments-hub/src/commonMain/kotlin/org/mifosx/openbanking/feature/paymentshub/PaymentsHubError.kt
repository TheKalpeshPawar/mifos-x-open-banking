/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentshub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun PaymentsHubError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(KptTheme.spacing.xl)
            .testTag(PaymentsHubTestTags.ERROR_SCREEN),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Couldn't load payments",
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.error,
        )
        Spacer(Modifier.height(KptTheme.spacing.sm))
        Text(
            text = message,
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(KptTheme.spacing.lg))
        MifosFilledPillButton(
            label = "Try again",
            onClick = onRetry,
            testTag = PaymentsHubTestTags.RETRY_BUTTON,
        )
    }
}
