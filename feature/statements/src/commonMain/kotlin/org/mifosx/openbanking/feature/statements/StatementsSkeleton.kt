/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.statements.generated.resources.Res
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_loading_accessibility
import template.core.base.designsystem.component.KptShimmerLoadingBox

private val RowMinHeight = 72.dp
private val RowHorizontalPadding = 16.dp
private val RowVerticalPadding = 12.dp
private val TextGap = 6.dp
private val TrailingGap = 8.dp
private val HeadlineBarWidth = 120.dp
private val HeadlineBarHeight = 16.dp
private val SupportingBarWidth = 180.dp
private val SupportingBarHeight = 14.dp
private val DateBarWidth = 90.dp
private val DateBarHeight = 12.dp
private val IconPlaceholderSize = 40.dp
private const val SKELETON_ROW_COUNT = 3

/**
 * Loading state: three skeleton rows shaped block-for-block like a content row — text bars on the
 * left, a date bar and a round icon placeholder on the right — with a divider between each.
 *
 * Three because it fills the viewport without implying a specific number of statements the account
 * may not have; shaped like the content it replaces so the layout does not jump when the data lands.
 */
@Composable
internal fun StatementsSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_statements_loading_accessibility)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(StatementsTestTags.LOADING_SKELETON)
            .semantics { contentDescription = description },
    ) {
        repeat(SKELETON_ROW_COUNT) { index ->
            SkeletonRow()
            if (index < SKELETON_ROW_COUNT - 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = RowHorizontalPadding),
                )
            }
        }
    }
}

@Composable
private fun SkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowHorizontalPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TextGap),
        ) {
            KptShimmerLoadingBox(modifier = Modifier.width(HeadlineBarWidth).height(HeadlineBarHeight))
            KptShimmerLoadingBox(
                modifier = Modifier.width(SupportingBarWidth).height(SupportingBarHeight),
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TrailingGap),
        ) {
            KptShimmerLoadingBox(modifier = Modifier.width(DateBarWidth).height(DateBarHeight))
            KptShimmerLoadingBox(modifier = Modifier.size(IconPlaceholderSize), shape = CircleShape)
        }
    }
}
