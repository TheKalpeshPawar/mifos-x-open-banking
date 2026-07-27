/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import org.mifosx.openbanking.feature.directdebits.generated.resources.Res
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_loading_accessibility
import template.core.base.designsystem.component.KptShimmerLoadingBox

private val SCREEN_PADDING = 16.dp
private val CARD_GAP = 12.dp
private val CHIP_GAP = 8.dp
private val ChipPlaceholderHeight = 32.dp
private val ActiveChipPlaceholderWidth = 96.dp
private val InactiveChipPlaceholderWidth = 104.dp
private val CardPlaceholderHeight = 132.dp
private const val CARD_PLACEHOLDER_COUNT = 4

/**
 * Loading state: a chip-row placeholder above four card placeholders.
 *
 * Shaped like the content it replaces rather than a bare spinner, so the layout does not jump when
 * the mandates land. Four is the placeholder count because it fills the viewport without implying
 * a specific number of mandates the account may not have.
 */
@Composable
internal fun DirectDebitsSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_direct_debits_loading_accessibility)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SCREEN_PADDING)
            .testTag(DirectDebitsTestTags.LOADING_SKELETON)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(CARD_GAP),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DirectDebitsTestTags.SKELETON_CHIP_ROW),
            horizontalArrangement = Arrangement.spacedBy(CHIP_GAP),
        ) {
            KptShimmerLoadingBox(
                modifier = Modifier.width(ActiveChipPlaceholderWidth).height(ChipPlaceholderHeight),
            )
            KptShimmerLoadingBox(
                modifier = Modifier.width(InactiveChipPlaceholderWidth).height(ChipPlaceholderHeight),
            )
        }
        repeat(CARD_PLACEHOLDER_COUNT) {
            KptShimmerLoadingBox(
                modifier = Modifier.fillMaxWidth().height(CardPlaceholderHeight),
            )
        }
    }
}
