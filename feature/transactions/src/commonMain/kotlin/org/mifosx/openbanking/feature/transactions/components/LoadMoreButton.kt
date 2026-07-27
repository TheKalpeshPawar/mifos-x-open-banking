/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.transactions.TransactionsTestTags
import org.mifosx.openbanking.feature.transactions.generated.resources.Res
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_load_more
import template.core.base.designsystem.theme.KptTheme

/** "Load more" affordance shown at the list foot when a next page cursor exists. */
@Composable
internal fun LoadMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(KptTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onClick, modifier = Modifier.testTag(TransactionsTestTags.LOAD_MORE)) {
            Text(stringResource(Res.string.feature_transactions_load_more))
        }
    }
}

/** Linear indeterminate indicator shown at the list foot while the next page is loading. */
@Composable
internal fun PaginationLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(KptTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag(TransactionsTestTags.PAGINATION_LOADER))
    }
}
