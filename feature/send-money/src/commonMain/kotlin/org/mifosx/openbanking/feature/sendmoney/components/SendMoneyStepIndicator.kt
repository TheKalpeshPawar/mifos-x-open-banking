/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mifosx.openbanking.feature.sendmoney.SendMoneyTestTags

private val DotWidth = 20.dp
private val DotHeight = 4.dp
private val DotGap = 6.dp
private val LabelGap = 8.dp
private val LetterSpacing = 0.04.sp

/**
 * "STEP 1 OF 3" beside three progress pills.
 *
 * Deliberately not a progress bar: the steps are discrete and countable, and a continuous bar would
 * imply a fraction of work done rather than which of three named stages the PSU is on.
 *
 * The pills are decorative — the whole row carries one spoken label, so a screen reader announces
 * the position once instead of reading three unlabelled shapes.
 */
@Composable
internal fun SendMoneyStepIndicator(
    current: Int,
    total: Int,
    label: String,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .testTag(SendMoneyTestTags.STEP_INDICATOR)
            .semantics { contentDescription = accessibilityLabel },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LabelGap),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = LetterSpacing),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DotGap)) {
            repeat(total) { index ->
                Row(
                    modifier = Modifier
                        .width(DotWidth)
                        .height(DotHeight)
                        .background(
                            color = if (index < current) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(percent = 50),
                        ),
                ) {}
            }
        }
    }
}
