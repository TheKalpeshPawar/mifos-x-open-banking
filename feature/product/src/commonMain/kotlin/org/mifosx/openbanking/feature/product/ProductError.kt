/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import org.mifosx.openbanking.feature.product.generated.resources.Res
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_error_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_error_consent_denied
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_error_network
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_error_session_expired
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_error_title
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_retry_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_retry_button
import org.mifosx.openbanking.feature.product.ui.ProductErrorKind
import template.core.base.designsystem.theme.KptTheme

private val ErrorIconSize = 48.dp

/**
 * Error state: the fetch failed in a way the customer may be able to act on.
 *
 * Retry is offered for every kind. Re-authenticating or re-consenting both happen elsewhere, but a retry
 * still costs nothing and succeeds once the customer has done so — whereas removing the button would
 * leave the screen with no way forward at all.
 */
@Composable
internal fun ProductError(
    kind: ProductErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retryLabel = stringResource(Res.string.feature_product_retry_a11y)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(KptTheme.spacing.lg)
            .testTag(ProductTestTags.ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = stringResource(Res.string.feature_product_error_a11y),
            tint = KptTheme.colorScheme.error,
            modifier = Modifier.size(ErrorIconSize),
        )
        Text(
            text = stringResource(Res.string.feature_product_error_title),
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = KptTheme.spacing.md),
        )
        Text(
            text = stringResource(kind.messageResource()),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = KptTheme.spacing.xs),
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .padding(top = KptTheme.spacing.lg)
                .testTag(ProductTestTags.RETRY_BUTTON)
                .semantics { contentDescription = retryLabel },
        ) {
            Text(text = stringResource(Res.string.feature_product_retry_button))
        }
    }
}

/** The customer-facing message for each failure, worded per `api.yaml` error_responses. */
private fun ProductErrorKind.messageResource() = when (this) {
    ProductErrorKind.TokenExpired -> Res.string.feature_product_error_session_expired
    ProductErrorKind.ConsentScope -> Res.string.feature_product_error_consent_denied
    ProductErrorKind.Network -> Res.string.feature_product_error_network
}
