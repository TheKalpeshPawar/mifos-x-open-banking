/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts

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

private val CardPlaceholderHeight = 96.dp
private const val CARD_PLACEHOLDER_COUNT = 3

/** Loading state: shimmer placeholders mirroring the account cards. */
@Composable
internal fun AccountsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(KptTheme.spacing.md)
            .testTag(AccountsTestTags.SKELETON),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        repeat(CARD_PLACEHOLDER_COUNT) {
            KptShimmerLoadingBox(modifier = Modifier.fillMaxWidth().height(CardPlaceholderHeight))
        }
    }
}
