/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentshub

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

@Composable
internal fun PaymentsHubSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = KptTheme.spacing.md)
            .testTag(PaymentsHubTestTags.SKELETON),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.lg),
    ) {
        // Header
        KptShimmerLoadingBox(
            modifier = Modifier.fillMaxWidth(0.4f).height(28.dp),
        )
        KptShimmerLoadingBox(
            modifier = Modifier.fillMaxWidth(0.7f).height(16.dp),
        )

        // Quick actions
        Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md)) {
            KptShimmerLoadingBox(
                modifier = Modifier.fillMaxWidth(0.25f).height(18.dp),
            )
            repeat(3) {
                Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md)) {
                    repeat(2) {
                        KptShimmerLoadingBox(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                        )
                    }
                }
            }
        }

        // Activity
        Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md)) {
            KptShimmerLoadingBox(
                modifier = Modifier.fillMaxWidth(0.3f).height(18.dp),
            )
            repeat(3) {
                KptShimmerLoadingBox(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                )
            }
        }
    }
}
