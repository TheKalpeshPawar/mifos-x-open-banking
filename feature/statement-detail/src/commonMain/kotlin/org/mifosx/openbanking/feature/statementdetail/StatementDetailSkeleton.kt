/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.statementdetail.generated.resources.Res
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_loading
import template.core.base.designsystem.component.KptShimmerLoadingBox

private val ScreenPadding = 16.dp
private val CardCornerRadius = 12.dp
private val CardHeight = 108.dp
private val SectionTopPadding = 20.dp
private val RowGap = 12.dp
private val HeaderBarWidth = 140.dp
private val HeaderBarHeight = 16.dp
private val RowBarWidth = 180.dp
private val RowBarHeight = 14.dp
private const val SKELETON_ROW_COUNT = 4

/**
 * Loading state: a header-card placeholder above a handful of line placeholders, shaped like the
 * content it replaces so the layout does not jump when the statement lands.
 */
@Composable
internal fun StatementDetailSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_statement_detail_loading)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding)
            .testTag(StatementDetailTestTags.LOADING_SKELETON)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        KptShimmerLoadingBox(
            modifier = Modifier.fillMaxWidth().height(CardHeight),
            shape = RoundedCornerShape(CardCornerRadius),
        )
        KptShimmerLoadingBox(
            modifier = Modifier
                .padding(top = SectionTopPadding)
                .width(HeaderBarWidth)
                .height(HeaderBarHeight),
        )
        repeat(SKELETON_ROW_COUNT) {
            KptShimmerLoadingBox(modifier = Modifier.width(RowBarWidth).height(RowBarHeight))
        }
    }
}
