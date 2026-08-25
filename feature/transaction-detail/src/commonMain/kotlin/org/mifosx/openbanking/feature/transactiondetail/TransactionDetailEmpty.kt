/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.Res
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_empty_accessibility
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_empty_body
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_empty_title
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_go_back
import template.core.base.designsystem.theme.KptTheme

/**
 * Empty state: the route's transaction id matched no record in an otherwise-successful response.
 *
 * A not-found, not a fault — so it uses the neutral surface-container well rather than the error
 * container, and offers a Go Back to return to the list the record dropped out of.
 */
@Composable
internal fun TransactionDetailEmpty(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_transaction_detail_empty_accessibility)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(KptTheme.spacing.lg)
            .testTag(TransactionDetailTestTags.EMPTY_STATE)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(DesignToken.sizes.avatarLarge)
                .background(color = KptTheme.colorScheme.surfaceContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = null,
                tint = KptTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignToken.sizes.iconExtraLarge),
            )
        }
        Text(
            text = stringResource(Res.string.feature_transaction_detail_empty_title),
            style = KptTheme.typography.headlineSmall,
            color = KptTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TransactionDetailTestTags.EMPTY_TITLE),
        )
        Text(
            text = stringResource(Res.string.feature_transaction_detail_empty_body),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TransactionDetailTestTags.EMPTY_BODY),
        )
        OutlinedButton(
            onClick = onBack,
            shape = DesignToken.shapes.pill,
            modifier = Modifier
                .fillMaxWidth()
                .height(DesignToken.sizes.buttonHeight)
                .testTag(TransactionDetailTestTags.EMPTY_BACK_BUTTON),
        ) {
            Text(
                text = stringResource(Res.string.feature_transaction_detail_go_back),
                style = KptTheme.typography.labelLarge,
            )
        }
    }
}
