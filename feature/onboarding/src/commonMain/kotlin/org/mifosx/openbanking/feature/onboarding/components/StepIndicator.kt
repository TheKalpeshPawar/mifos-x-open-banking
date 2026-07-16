/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import template.core.base.designsystem.theme.KptTheme

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(current = currentStep.toFloat(), range = 1f..totalSteps.toFloat())
                contentDescription = "Step $currentStep of $totalSteps"
            }
            .testTag("onboarding_step_indicator_$currentStep"),
    ) {
        for (step in 1..totalSteps) {
            val isActive = step == currentStep
            val isCompleted = step < currentStep
            Box(
                modifier = Modifier
                    .then(if (isActive) Modifier.width(24.dp) else Modifier.size(8.dp))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> KptTheme.colorScheme.primary
                            isCompleted -> KptTheme.colorScheme.secondaryContainer
                            else -> KptTheme.colorScheme.outlineVariant
                        },
                    ),
            )
            if (step < totalSteps) Spacer(Modifier.width(KptTheme.spacing.sm))
        }
    }
}
