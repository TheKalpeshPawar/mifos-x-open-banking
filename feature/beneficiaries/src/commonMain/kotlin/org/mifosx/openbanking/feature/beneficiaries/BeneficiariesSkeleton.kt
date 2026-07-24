/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

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
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.Res
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_loading_a11y

/**
 * Loading state: a single centred spinner, matching the design's circular indicator.
 *
 * The search field is deliberately absent here — there is nothing to filter yet, and offering an
 * input that cannot act would be a dead control.
 */
@Composable
internal fun BeneficiariesSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_beneficiaries_loading_a11y)
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(BeneficiariesTestTags.LOADING)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
