/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus

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
import androidx.compose.material.icons.filled.ErrorOutline
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.Res
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_error_consent_revoked
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_error_network
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_error_not_found
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_error_title
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_error_token_expired
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_retry
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusErrorKind

private val ContentPadding = 24.dp
private val LineGap = 16.dp
private val IconWellSize = 88.dp
private val IconSize = 40.dp
private val BodyMaxWidth = 320.dp
private val ButtonTopGap = 8.dp

/**
 * The status could not be read.
 *
 * Retry is offered for every kind including [PaymentStatusErrorKind.PaymentNotFound]: a read is free
 * and reversible, so unlike the send flow there is no failure here where trying again could do harm.
 */
@Composable
internal fun PaymentStatusError(
    kind: PaymentStatusErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.feature_payment_status_error_title)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(PaymentStatusTestTags.ERROR_STATE)
            .semantics { contentDescription = title },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconWellSize)
                .background(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(IconSize),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(kind.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = BodyMaxWidth),
        )

        MifosFilledPillButton(
            label = stringResource(Res.string.feature_payment_status_retry),
            onClick = onRetry,
            modifier = Modifier.padding(top = ButtonTopGap),
            testTag = PaymentStatusTestTags.RETRY_BUTTON,
        )
    }
}

private fun PaymentStatusErrorKind.bodyResource(): StringResource = when (this) {
    PaymentStatusErrorKind.PaymentNotFound -> Res.string.feature_payment_status_error_not_found
    PaymentStatusErrorKind.TokenExpired -> Res.string.feature_payment_status_error_token_expired
    PaymentStatusErrorKind.ConsentRevoked -> Res.string.feature_payment_status_error_consent_revoked
    PaymentStatusErrorKind.NetworkError -> Res.string.feature_payment_status_error_network
}
