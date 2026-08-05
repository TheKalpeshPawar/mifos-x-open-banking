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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_success_amount
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_success_body
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_success_payment_id
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_success_status
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_success_title
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_view_status

private val ContentPadding = 24.dp
private val LineGap = 12.dp
private val IconWellSize = 96.dp
private val IconSize = 46.dp
private val IconBottomGap = 8.dp
private val BodyMaxWidth = 320.dp
private val ChipPaddingHorizontal = 12.dp
private val ChipPaddingVertical = 4.dp
private val ChipShape = RoundedCornerShape(percent = 50)
private val ButtonTopGap = 16.dp

/**
 * The payment has been accepted, which is not the same as settled.
 *
 * The illustration is a clock in the secondary container, not a check mark in a celebratory hue.
 * `AcceptedSettlementInProcess` means the bank has taken the instruction and the money has not
 * arrived; a tick would state that it had, and the PSU would stop watching for a payment that can
 * still be rejected.
 */
@Composable
internal fun SendMoneySuccess(
    amountLabel: String,
    creditorName: String,
    paymentId: String,
    onViewStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(SendMoneyTestTags.SUCCESS_STATE),
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconWellSize)
                .padding(bottom = IconBottomGap)
                .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(IconSize),
            )
        }

        Text(
            text = stringResource(Res.string.feature_send_money_success_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(Res.string.feature_send_money_success_amount, amountLabel, creditorName),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(SendMoneyTestTags.SUCCESS_AMOUNT),
        )

        Text(
            text = stringResource(Res.string.feature_send_money_success_status),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = ChipShape)
                .padding(horizontal = ChipPaddingHorizontal, vertical = ChipPaddingVertical)
                .testTag(SendMoneyTestTags.SUCCESS_STATUS_CHIP),
        )

        Text(
            text = stringResource(Res.string.feature_send_money_success_body, creditorName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = BodyMaxWidth),
        )

        Text(
            text = stringResource(Res.string.feature_send_money_success_payment_id, paymentId),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(SendMoneyTestTags.SUCCESS_PAYMENT_ID),
        )

        MifosFilledPillButton(
            label = stringResource(Res.string.feature_send_money_view_status),
            onClick = onViewStatus,
            modifier = Modifier.padding(top = ButtonTopGap),
            testTag = SendMoneyTestTags.VIEW_PAYMENT_STATUS_BUTTON,
        )
    }
}
