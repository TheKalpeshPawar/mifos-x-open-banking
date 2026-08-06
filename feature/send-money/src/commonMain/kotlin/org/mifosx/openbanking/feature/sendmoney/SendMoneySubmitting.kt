/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_submitting_amount
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_submitting_awaiting
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_submitting_lock
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_submitting_staging
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyStage

private val ContentPadding = 24.dp
private val LineGap = 16.dp
private val IndicatorSize = 56.dp
private val IndicatorBottomGap = 8.dp
private val LockIconSize = 16.dp
private val LockGap = 8.dp
private val BodyMaxWidth = 300.dp

/**
 * Submission in flight.
 *
 * There is no button here at all — not a disabled one. Confirming moves the screen to this state,
 * which unmounts the control entirely, and a control that does not exist cannot be tapped twice.
 * That is a stronger guarantee than a disabled attribute for the one screen in this app where a
 * double tap would move money twice.
 */
@Composable
internal fun SendMoneySubmitting(
    stage: SendMoneyStage,
    amountLabel: String,
    creditorName: String,
    modifier: Modifier = Modifier,
) {
    val caption = when (stage) {
        SendMoneyStage.StagingConsent -> stringResource(Res.string.feature_send_money_submitting_staging)
        SendMoneyStage.AwaitingAuthorisation -> stringResource(Res.string.feature_send_money_submitting_awaiting)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(SendMoneyTestTags.SUBMITTING_INDICATOR),
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(IndicatorSize)
                .padding(bottom = IndicatorBottomGap),
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = stringResource(Res.string.feature_send_money_submitting_amount, amountLabel, creditorName),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(SendMoneyTestTags.SUBMITTING_AMOUNT),
        )

        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = BodyMaxWidth),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LockGap),
            modifier = Modifier.testTag(SendMoneyTestTags.SUBMITTING_LOCK_NOTE),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(LockIconSize),
            )
            Text(
                text = stringResource(Res.string.feature_send_money_submitting_lock),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
