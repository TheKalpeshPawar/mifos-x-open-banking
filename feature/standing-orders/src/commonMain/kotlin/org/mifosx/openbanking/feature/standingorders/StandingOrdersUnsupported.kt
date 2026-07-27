/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.standingorders.generated.resources.Res
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_unsupported_accessibility
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_unsupported_title

private val IconWellSize = 72.dp
private val IconSize = 40.dp
private val BodyMaxWidth = 280.dp
private val ContentPadding = 24.dp
private val LineGap = 8.dp

/**
 * Unsupported state: the account's HSBC product does not offer standing orders at all.
 *
 * Mirrors [StandingOrdersEmpty]'s neutral treatment rather than the error container, for the same
 * reason — this is a definitive answer from the bank, not a fault. It offers no Retry, because no
 * number of retries will give a savings account standing orders.
 *
 * @param message The bank's own words, shown verbatim. Falls back to nothing when the response
 *   carried no message; the title alone still tells the user where they stand.
 */
@Composable
internal fun StandingOrdersUnsupported(
    message: String,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_standing_orders_unsupported_accessibility)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(StandingOrdersTestTags.UNSUPPORTED_STATE)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconWellSize)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(Res.string.feature_standing_orders_unsupported_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(StandingOrdersTestTags.UNSUPPORTED_TITLE),
        )
        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = BodyMaxWidth)
                    .testTag(StandingOrdersTestTags.UNSUPPORTED_BODY),
            )
        }
    }
}
