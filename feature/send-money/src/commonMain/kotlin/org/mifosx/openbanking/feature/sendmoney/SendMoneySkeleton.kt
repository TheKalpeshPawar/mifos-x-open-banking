/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

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
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_loading_a11y
import template.core.base.designsystem.component.KptShimmerLoadingBox

private val ScreenPadding = 16.dp
private val SectionGap = 16.dp
private val StepIndicatorHeight = 20.dp
private val StepIndicatorWidth = 160.dp
private val HeadingHeight = 20.dp
private val HeadingWidth = 220.dp
private val PickerListHeight = 144.dp
private val CreditorListHeight = 216.dp

/**
 * Loading state.
 *
 * Shaped like the recipient step it replaces — indicator, heading, payer list, heading, payee list —
 * so nothing jumps when the accounts and payees land. The preview for this state draws a centred
 * spinner, but every other screen in this app uses a shimmer skeleton and the convention wins:
 * a spinner here would be the one screen that loads differently from the rest.
 */
@Composable
internal fun SendMoneySkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_send_money_loading_a11y)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ScreenPadding)
            .testTag(SendMoneyTestTags.SKELETON)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        KptShimmerLoadingBox(
            modifier = Modifier.width(StepIndicatorWidth).height(StepIndicatorHeight),
        )
        KptShimmerLoadingBox(
            modifier = Modifier.width(HeadingWidth).height(HeadingHeight),
        )
        KptShimmerLoadingBox(
            modifier = Modifier.fillMaxWidth().height(PickerListHeight),
        )
        KptShimmerLoadingBox(
            modifier = Modifier.width(HeadingWidth).height(HeadingHeight),
        )
        KptShimmerLoadingBox(
            modifier = Modifier.fillMaxWidth().height(CreditorListHeight),
        )
    }
}
