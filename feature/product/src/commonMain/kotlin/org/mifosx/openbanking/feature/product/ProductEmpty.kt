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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.product.generated.resources.Res
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_empty_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_empty_body
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_empty_title
import template.core.base.designsystem.theme.KptTheme

private val EmptyIconSize = 48.dp

/**
 * Empty state: this account publishes no product terms.
 *
 * Deliberately **informational**, not an error — `info_outline` rather than `error_outline`, and no Retry.
 * It is the normal answer for GlobalMoney, Savings and CreditCard accounts, which OBIE has no `OBProduct2`
 * entry for, and it is also where a 404 lands.
 */
@Composable
internal fun ProductEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(KptTheme.spacing.lg)
            .testTag(ProductTestTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = stringResource(Res.string.feature_product_empty_a11y),
            tint = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(EmptyIconSize),
        )
        Text(
            text = stringResource(Res.string.feature_product_empty_title),
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = KptTheme.spacing.md),
        )
        Text(
            text = stringResource(Res.string.feature_product_empty_body),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = KptTheme.spacing.xs),
        )
    }
}
