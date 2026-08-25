/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.feature.paymentstatus.PaymentStatusTestTags
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.Res
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_step_approved
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_step_completed
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_step_rejected
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_step_request_created
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_step_settling
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_step_submitted
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_step_waiting
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_timeline
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStepState
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentTimelineEntry
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentTimelineStep
import template.core.base.designsystem.theme.KptTheme

/**
 * The four stages a payment passes through, newest first.
 *
 * Newest first because the only stage anyone is waiting on is the last one, and a chronological
 * list would put it furthest from the eye. The connector runs downward from each marker into the
 * older stage beneath it, so the column still reads as one thread rather than four unrelated rows.
 *
 * A stage with no timestamp renders its label alone. Nothing is substituted: approval and
 * submission times exist only where this app observed them, and a payment made on another device
 * genuinely has none.
 */
@Composable
internal fun PaymentTimeline(
    entries: List<PaymentTimelineEntry>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        Text(
            text = stringResource(Res.string.feature_payment_status_timeline).uppercase(),
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(KptTheme.shapes.medium)
                .background(KptTheme.colorScheme.surfaceContainerLow)
                .padding(KptTheme.spacing.md)
                .testTag(PaymentStatusTestTags.TIMELINE),
        ) {
            entries.forEachIndexed { index, entry ->
                TimelineRow(entry = entry, hasOlderBelow = index != entries.lastIndex)
            }
        }
    }
}

@Composable
private fun TimelineRow(entry: PaymentTimelineEntry, hasOlderBelow: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PaymentStatusTestTags.timelineStep(entry.step)),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        Column(
            modifier = Modifier.width(DesignToken.sizes.iconMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepMarker(entry)
            if (hasOlderBelow) {
                Box(
                    modifier = Modifier
                        .width(DesignToken.strokes.thick)
                        .height(DesignToken.sizes.iconSmall)
                        .background(KptTheme.colorScheme.outlineVariant),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (hasOlderBelow) KptTheme.spacing.md else 0.dp),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(entry.labelResource()),
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurface,
            )
            // Omitted, not blanked: a stage nothing observed is undated, and an empty value beside a
            // label reads as data we lost rather than data we were never given.
            if (entry.timestamp.isNotBlank()) {
                Text(
                    text = entry.timestamp,
                    style = KptTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepMarker(entry: PaymentTimelineEntry) {
    val scheme = KptTheme.colorScheme
    val fill: Color = when (entry.state) {
        PaymentStepState.Done -> scheme.tertiary
        PaymentStepState.Current -> scheme.secondary
        PaymentStepState.Pending -> Color.Transparent
        PaymentStepState.Failed -> scheme.error
    }

    Box(
        modifier = Modifier
            .size(DesignToken.sizes.iconSmall)
            .clip(CircleShape)
            .background(fill)
            .border(DesignToken.strokes.thick, scheme.outlineVariant, CircleShape)
            .testTag(PaymentStatusTestTags.timelineState(entry.step, entry.state)),
        contentAlignment = Alignment.Center,
    ) {
        when (entry.state) {
            PaymentStepState.Done -> MarkerIcon(Icons.Filled.Check, scheme.onTertiary)
            PaymentStepState.Failed -> MarkerIcon(Icons.Filled.Close, scheme.onError)
            // Current and Pending are the colour of the dot alone — there is no outcome to glyph
            // yet, and a tick on a stage still running would be the optimism this screen refuses.
            PaymentStepState.Current, PaymentStepState.Pending -> Unit
        }
    }
}

@Composable
private fun MarkerIcon(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(DesignToken.sizes.iconExtraSmall),
    )
}

/**
 * The last stage is named by its outcome, not by the stage it would have been.
 *
 * A rejected payment labelled "Completed" and marked failed says two contradictory things at once,
 * and the one people read is the word.
 */
private fun PaymentTimelineEntry.labelResource(): StringResource = when (step) {
    PaymentTimelineStep.RequestCreated -> Res.string.feature_payment_status_step_request_created
    PaymentTimelineStep.ApprovedAtBank -> Res.string.feature_payment_status_step_approved
    PaymentTimelineStep.Submitted -> Res.string.feature_payment_status_step_submitted
    PaymentTimelineStep.Completed -> when (state) {
        PaymentStepState.Failed -> Res.string.feature_payment_status_step_rejected
        PaymentStepState.Current -> Res.string.feature_payment_status_step_settling
        PaymentStepState.Pending -> Res.string.feature_payment_status_step_waiting
        PaymentStepState.Done -> Res.string.feature_payment_status_step_completed
    }
}
