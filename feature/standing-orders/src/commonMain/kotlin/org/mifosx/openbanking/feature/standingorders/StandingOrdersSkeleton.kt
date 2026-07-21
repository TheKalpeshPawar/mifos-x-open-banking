/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.standingorders.generated.resources.Res
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_loading_accessibility
import template.core.base.designsystem.component.KptShimmerLoadingBox

private val SCREEN_PADDING = 16.dp
private val CARD_GAP = 12.dp
private val SummaryPlaceholderHeight = 20.dp
private val SummaryPlaceholderWidth = 168.dp
private val CardPlaceholderHeight = 176.dp
private const val CARD_PLACEHOLDER_COUNT = 3

/**
 * Loading state: a summary-bar placeholder above three card placeholders.
 *
 * Shaped like the content it replaces rather than a bare spinner, so the layout does not jump when
 * the orders land. The card placeholder is taller than the direct-debits one because a standing
 * order card carries more lines; three fills the viewport without implying a specific number of
 * orders the account may not have.
 */
@Composable
internal fun StandingOrdersSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_standing_orders_loading_accessibility)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SCREEN_PADDING)
            .testTag(StandingOrdersTestTags.LOADING_SKELETON)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(CARD_GAP),
    ) {
        KptShimmerLoadingBox(
            modifier = Modifier
                .width(SummaryPlaceholderWidth)
                .height(SummaryPlaceholderHeight)
                .testTag(StandingOrdersTestTags.SKELETON_SUMMARY),
        )
        repeat(CARD_PLACEHOLDER_COUNT) {
            KptShimmerLoadingBox(
                modifier = Modifier.fillMaxWidth().height(CardPlaceholderHeight),
            )
        }
    }
}
