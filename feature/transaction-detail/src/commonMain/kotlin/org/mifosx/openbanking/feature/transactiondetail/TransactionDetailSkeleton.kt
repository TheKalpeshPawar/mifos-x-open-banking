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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.Res
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_loading

/**
 * Loading state: a single centred spinner.
 *
 * The detail record resolves from the account's already-fetched transaction list in the common case,
 * so this is usually momentary; a plain circular indicator matches the design rather than a shaped
 * skeleton that would only flash.
 */
@Composable
internal fun TransactionDetailSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_transaction_detail_loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(TransactionDetailTestTags.LOADING_INDICATOR)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
