/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus

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
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.Res
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_loading_a11y
import template.core.base.designsystem.component.KptShimmerLoadingBox

private val ScreenPadding = 16.dp
private val SectionGap = 16.dp
private val SummaryCardHeight = 180.dp
private val NoteHeight = 96.dp
private val HeadingHeight = 16.dp
private val HeadingWidth = 96.dp
private val DetailsHeight = 224.dp
private val ButtonHeight = 48.dp

/**
 * Loading state, shaped like the content it replaces so nothing jumps when the status lands.
 */
@Composable
internal fun PaymentStatusSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_payment_status_loading_a11y)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ScreenPadding)
            .testTag(PaymentStatusTestTags.SKELETON)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        KptShimmerLoadingBox(modifier = Modifier.fillMaxWidth().height(SummaryCardHeight))
        KptShimmerLoadingBox(modifier = Modifier.fillMaxWidth().height(NoteHeight))
        KptShimmerLoadingBox(modifier = Modifier.width(HeadingWidth).height(HeadingHeight))
        KptShimmerLoadingBox(modifier = Modifier.fillMaxWidth().height(DetailsHeight))
        KptShimmerLoadingBox(modifier = Modifier.fillMaxWidth().height(ButtonHeight))
    }
}
