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
import org.mifosx.openbanking.feature.product.generated.resources.Res
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_loading_a11y

/**
 * Loading state: a centred circular indicator.
 *
 * `ui.yaml` specifies a `progress_indicator`, not the shimmer placeholder some sibling screens use, so
 * this stays a plain spinner. The file keeps the `Skeleton` name the feature anatomy uses for the loading
 * state across the codebase.
 */
@Composable
internal fun ProductSkeleton(modifier: Modifier = Modifier) {
    val loadingLabel = stringResource(Res.string.feature_product_loading_a11y)
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(ProductTestTags.LOADING)
            .semantics { contentDescription = loadingLabel },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
