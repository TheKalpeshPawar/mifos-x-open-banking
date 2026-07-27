/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentcallback.steps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.consentcallback.generated.resources.Res
import org.mifosx.openbanking.feature.consentcallback.generated.resources.feature_callback_connected_body
import org.mifosx.openbanking.feature.consentcallback.generated.resources.feature_callback_connected_title
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun SuccessStep(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().testTag("callback_content"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(64.dp), tint = KptTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.feature_callback_connected_title),
                style = KptTheme.typography.headlineSmall,
                color = KptTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(KptTheme.spacing.sm))
            Text(
                stringResource(Res.string.feature_callback_connected_body),
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }
    }
}
