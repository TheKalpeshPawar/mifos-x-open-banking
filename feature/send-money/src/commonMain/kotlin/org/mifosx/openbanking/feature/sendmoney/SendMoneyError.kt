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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.core.ui.components.MifosTonalPillButton
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_change_payer
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_edit_amount
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_consent_mismatch
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_consent_not_authorised
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_consent_revoked
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_insufficient_funds
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_invalid_field
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_network
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_outside_limits
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_payer_not_supported
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_rate_limited
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_reference
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_signature_missing
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_title
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_error_token_expired
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_reauthorise
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_retry
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_view_consents
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyErrorKind
import org.mifosx.openbanking.feature.sendmoney.ui.isRetryable
import org.mifosx.openbanking.feature.sendmoney.ui.needsAmountChange
import org.mifosx.openbanking.feature.sendmoney.ui.needsReauthorisation
import template.core.base.designsystem.theme.KptTheme

/**
 * The payment failed, and which recoveries appear depends on why.
 *
 * The four are not interchangeable and are never all shown at once: retrying a revoked consent will
 * never succeed, and re-authorising an insufficient balance does not add money to the account.
 * `SignatureMissing` deliberately offers nothing — it is a defect in this app, so every button
 * would be a false promise; the support reference is the only useful thing on screen.
 */
@Composable
internal fun SendMoneyError(
    kind: SendMoneyErrorKind,
    supportReference: String?,
    onRetry: () -> Unit,
    onReauthorise: () -> Unit,
    onViewConsents: () -> Unit,
    onEditAmount: () -> Unit,
    onChangePayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.feature_send_money_error_title)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(KptTheme.spacing.lg)
            .testTag(SendMoneyTestTags.ERROR_STATE)
            .semantics { contentDescription = title },
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(DesignToken.sizes.avatarXLarge)
                .padding(bottom = KptTheme.spacing.sm)
                .background(color = KptTheme.colorScheme.errorContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = KptTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(DesignToken.sizes.iconHuge),
            )
        }

        Text(
            text = title,
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(kind.bodyResource()),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (supportReference != null) {
            Text(
                text = stringResource(Res.string.feature_send_money_error_reference, supportReference),
                style = KptTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = KptTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(SendMoneyTestTags.ERROR_SUPPORT_REFERENCE),
            )
        }

        SendMoneyErrorActions(
            kind = kind,
            onRetry = onRetry,
            onReauthorise = onReauthorise,
            onViewConsents = onViewConsents,
            onEditAmount = onEditAmount,
            onChangePayer = onChangePayer,
        )
    }
}

@Composable
private fun SendMoneyErrorActions(
    kind: SendMoneyErrorKind,
    onRetry: () -> Unit,
    onReauthorise: () -> Unit,
    onViewConsents: () -> Unit,
    onEditAmount: () -> Unit,
    onChangePayer: () -> Unit,
) {
    if (kind.isRetryable) {
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_send_money_retry),
            onClick = onRetry,
            modifier = Modifier.padding(top = KptTheme.spacing.sm),
            testTag = SendMoneyTestTags.RETRY_BUTTON,
        )
    }
    if (kind.needsReauthorisation) {
        MifosTonalPillButton(
            label = stringResource(Res.string.feature_send_money_reauthorise),
            onClick = onReauthorise,
            modifier = Modifier.padding(top = KptTheme.spacing.sm),
            testTag = SendMoneyTestTags.REAUTHORISE_BUTTON,
        )
    }
    if (kind == SendMoneyErrorKind.ConsentRevoked) {
        MifosTonalPillButton(
            label = stringResource(Res.string.feature_send_money_view_consents),
            onClick = onViewConsents,
            modifier = Modifier.padding(top = KptTheme.spacing.sm),
            testTag = SendMoneyTestTags.VIEW_CONSENTS_BUTTON,
        )
    }
    if (kind.needsAmountChange) {
        MifosTonalPillButton(
            label = stringResource(Res.string.feature_send_money_edit_amount),
            onClick = onEditAmount,
            modifier = Modifier.padding(top = KptTheme.spacing.sm),
            testTag = SendMoneyTestTags.EDIT_AMOUNT_BUTTON,
        )
    }
    // The refused account is already gone from the picker by the time this is tapped — the registry
    // removed it when the bank refused it — so the customer returns to a list they can succeed from.
    if (kind == SendMoneyErrorKind.PayerNotSupported) {
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_send_money_change_payer),
            onClick = onChangePayer,
            modifier = Modifier.padding(top = KptTheme.spacing.sm),
            testTag = SendMoneyTestTags.CHANGE_PAYER_BUTTON,
        )
    }
}

private fun SendMoneyErrorKind.bodyResource(): StringResource = when (this) {
    SendMoneyErrorKind.SignatureMissing -> Res.string.feature_send_money_error_signature_missing
    SendMoneyErrorKind.ConsentNotAuthorised -> Res.string.feature_send_money_error_consent_not_authorised
    SendMoneyErrorKind.ConsentMismatch -> Res.string.feature_send_money_error_consent_mismatch
    SendMoneyErrorKind.OutsideControlParameters -> Res.string.feature_send_money_error_outside_limits
    SendMoneyErrorKind.InvalidField -> Res.string.feature_send_money_error_invalid_field
    SendMoneyErrorKind.ConsentRevoked -> Res.string.feature_send_money_error_consent_revoked
    SendMoneyErrorKind.TokenExpired -> Res.string.feature_send_money_error_token_expired
    SendMoneyErrorKind.RateLimited -> Res.string.feature_send_money_error_rate_limited
    SendMoneyErrorKind.InsufficientFunds -> Res.string.feature_send_money_error_insufficient_funds
    SendMoneyErrorKind.PayerNotSupported -> Res.string.feature_send_money_error_payer_not_supported
    SendMoneyErrorKind.NetworkError -> Res.string.feature_send_money_error_network
}
