/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import template.core.base.designsystem.component.KptShimmerLoadingBox
import template.core.base.designsystem.theme.KptTheme

private val SummaryHeight = 56.dp
private val ChipsHeight = 40.dp
private val SearchHeight = 56.dp
private val RowHeight = 64.dp
private const val ROW_PLACEHOLDER_COUNT = 5

/** Loading state: shimmer placeholders mirroring the summary, filters, search and transaction rows. */
@Composable
internal fun TransactionsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(KptTheme.spacing.md)
            .testTag(TransactionsTestTags.SKELETON),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        KptShimmerLoadingBox(modifier = Modifier.fillMaxWidth().height(SummaryHeight))
        KptShimmerLoadingBox(modifier = Modifier.fillMaxWidth().height(ChipsHeight))
        KptShimmerLoadingBox(modifier = Modifier.fillMaxWidth().height(SearchHeight))
        repeat(ROW_PLACEHOLDER_COUNT) {
            KptShimmerLoadingBox(modifier = Modifier.fillMaxWidth().height(RowHeight))
        }
    }
}
