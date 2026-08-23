/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** A numbered step indicator with a connecting bar and trailing content. */
@Composable
fun MFStepProcess(
    stepNumber: String,
    activateColor: Color,
    deactivateColor: Color,
    modifier: Modifier = Modifier,
    isLastStep: Boolean = false,
    processState: StepProcessState = StepProcessState.INACTIVE,
    content: @Composable (Modifier) -> Unit,
) {
    var barHeight by remember { mutableStateOf(0.dp) }
    val localDensity = LocalDensity.current

    Row(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                modifier = Modifier
                    .size(DesignToken.sizes.step)
                    .clip(CircleShape)
                    .background(
                        color = if (processState == StepProcessState.INACTIVE) {
                            deactivateColor
                        } else {
                            activateColor
                        },
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (processState == StepProcessState.COMPLETED) "✔" else stepNumber,
                )
            }
            if (!isLastStep) {
                Box(
                    modifier = Modifier
                        .height(barHeight)
                        .width(KptTheme.spacing.xs)
                        .background(
                            color = if (processState == StepProcessState.INACTIVE) {
                                deactivateColor
                            } else {
                                activateColor
                            },
                        ),
                )
            }
        }
        content(
            Modifier
                .padding(start = KptTheme.spacing.sm, end = KptTheme.spacing.xs)
                .onGloballyPositioned { barHeight = with(localDensity) { it.size.height.toDp() } },
        )
    }
}

enum class StepProcessState {
    COMPLETED,
    ACTIVE,
    INACTIVE,
}

fun getStepState(targetStep: Int, currentStep: Int): StepProcessState {
    return when {
        currentStep == targetStep -> StepProcessState.ACTIVE
        currentStep > targetStep -> StepProcessState.COMPLETED
        else -> StepProcessState.INACTIVE
    }
}

@Preview
@Composable
private fun MFStepProcessPreview() {
    MifosXOpenBankingTheme {
        MFStepProcess(
            stepNumber = "1",
            activateColor = KptTheme.colorScheme.primary,
            deactivateColor = KptTheme.colorScheme.surfaceVariant,
            isLastStep = false,
            processState = StepProcessState.ACTIVE,
        ) {
            Text(text = "Step 1")
        }
    }
}
