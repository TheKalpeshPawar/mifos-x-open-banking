/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.accountdetail.generated.resources.Res
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_loading_label
import template.core.base.designsystem.component.KptShimmerLoadingBox
import template.core.base.designsystem.theme.KptTheme

private val HeaderPlaceholderHeight = 120.dp
private val SectionPlaceholderHeight = 32.dp
private val ListPlaceholderHeight = 160.dp

/**
 * Loading state: a spinner with a spoken label, then shimmer blocks standing in for the header
 * card, the Balances section heading and the balance list.
 */
@Composable
internal fun AccountDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(KptTheme.spacing.md)
            .testTag(AccountDetailTestTags.LOADING_SKELETON),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(AccountDetailTestTags.LOADING_SPINNER),
        )
        Text(
            text = stringResource(Res.string.feature_account_detail_loading_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        KptShimmerLoadingBox(
            modifier = Modifier.fillMaxWidth().height(HeaderPlaceholderHeight),
        )
        KptShimmerLoadingBox(
            modifier = Modifier.fillMaxWidth(SECTION_PLACEHOLDER_WIDTH).height(SectionPlaceholderHeight),
        )
        KptShimmerLoadingBox(
            modifier = Modifier.fillMaxWidth().height(ListPlaceholderHeight),
        )
    }
}

private const val SECTION_PLACEHOLDER_WIDTH = 0.6f
