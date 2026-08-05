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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.banking.payment.PaymentDisposition
import org.mifosx.openbanking.core.ui.components.MifosTonalPillButton
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.Res
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_completed
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_details
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_failed
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_from
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_in_progress
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_in_progress_note
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_new_payment
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_payment_id
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_reference
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_reference_empty
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_refresh
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_submitted
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_to
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusAction
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusUiState

private val ScreenPadding = 16.dp
private val SectionGap = 16.dp
private val CardShape = RoundedCornerShape(16.dp)
private val CardPadding = 24.dp
private val NoteShape = RoundedCornerShape(12.dp)
private val NotePadding = 16.dp
private val NoteGap = 12.dp
private val NoteIconSize = 24.dp
private val ChipShape = RoundedCornerShape(percent = 50)
private val ChipPaddingHorizontal = 12.dp
private val ChipPaddingVertical = 6.dp
private val ChipGap = 6.dp
private val ChipIconSize = 16.dp
private val RowPadding = 16.dp
private val TitleGap = 8.dp

/**
 * The payment as it currently stands: what was paid, its disposition, and the details behind it.
 *
 * The disposition drives the colour throughout. In-flight is deliberately not error-coloured —
 * "accepted, not yet settled" is the normal answer for a fresh payment, and rendering it as a fault
 * would send people chasing a problem that does not exist.
 */
@Composable
internal fun PaymentStatusContent(
    state: PaymentStatusUiState.Content,
    onAction: (PaymentStatusAction) -> Unit,
    onStartNewPayment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        SummaryCard(state)

        if (state.inProgress) {
            InProgressNote()
        }

        DetailsSection(state)

        MifosTonalPillButton(
            label = stringResource(Res.string.feature_payment_status_refresh),
            onClick = { onAction(PaymentStatusAction.RefreshStatus) },
            icon = Icons.Filled.Refresh,
            testTag = PaymentStatusTestTags.REFRESH_BUTTON,
            enabled = !state.refreshing,
        )

        MifosTonalPillButton(
            label = stringResource(Res.string.feature_payment_status_new_payment),
            onClick = onStartNewPayment,
            testTag = PaymentStatusTestTags.NEW_PAYMENT_BUTTON,
        )
    }
}

@Composable
private fun SummaryCard(state: PaymentStatusUiState.Content) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(CardPadding)
            .testTag(PaymentStatusTestTags.SUMMARY_CARD),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TitleGap),
    ) {
        Text(
            text = state.amountLabel,
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(PaymentStatusTestTags.AMOUNT),
        )
        Text(
            text = stringResource(Res.string.feature_payment_status_to, state.creditorName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        StatusChip(state.disposition)
    }
}

@Composable
private fun StatusChip(disposition: PaymentDisposition) {
    val container: Color
    val onContainer: Color
    val icon: ImageVector
    when (disposition) {
        PaymentDisposition.InProgress -> {
            container = MaterialTheme.colorScheme.secondaryContainer
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer
            icon = Icons.Filled.Schedule
        }

        PaymentDisposition.TerminalSuccess -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer
            icon = Icons.Filled.CheckCircle
        }

        PaymentDisposition.TerminalFailure -> {
            container = MaterialTheme.colorScheme.errorContainer
            onContainer = MaterialTheme.colorScheme.onErrorContainer
            icon = Icons.Filled.ErrorOutline
        }
    }

    Row(
        modifier = Modifier
            .clip(ChipShape)
            .background(container)
            .padding(horizontal = ChipPaddingHorizontal, vertical = ChipPaddingVertical)
            .testTag(PaymentStatusTestTags.STATUS_CHIP),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onContainer,
            modifier = Modifier.size(ChipIconSize),
        )
        Text(
            text = stringResource(disposition.labelResource()),
            style = MaterialTheme.typography.labelLarge,
            color = onContainer,
        )
    }
}

@Composable
private fun InProgressNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoteShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(NotePadding)
            .testTag(PaymentStatusTestTags.IN_PROGRESS_NOTE),
        horizontalArrangement = Arrangement.spacedBy(NoteGap),
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(NoteIconSize),
        )
        Text(
            text = stringResource(Res.string.feature_payment_status_in_progress_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun DetailsSection(state: PaymentStatusUiState.Content) {
    Column(verticalArrangement = Arrangement.spacedBy(TitleGap)) {
        Text(
            text = stringResource(Res.string.feature_payment_status_details).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(NoteShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .testTag(PaymentStatusTestTags.DETAILS_LIST),
        ) {
            DetailRow(
                label = stringResource(Res.string.feature_payment_status_reference),
                value = state.reference.ifBlank {
                    stringResource(Res.string.feature_payment_status_reference_empty)
                },
                tag = PaymentStatusTestTags.DETAIL_REFERENCE,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                label = stringResource(Res.string.feature_payment_status_from),
                value = state.debtorLabel,
                tag = PaymentStatusTestTags.DETAIL_FROM,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                label = stringResource(Res.string.feature_payment_status_submitted),
                value = state.submittedAt,
                tag = PaymentStatusTestTags.DETAIL_SUBMITTED,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                label = stringResource(Res.string.feature_payment_status_payment_id),
                value = state.paymentId,
                tag = PaymentStatusTestTags.DETAIL_PAYMENT_ID,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(RowPadding)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

private fun PaymentDisposition.labelResource() = when (this) {
    PaymentDisposition.InProgress -> Res.string.feature_payment_status_in_progress
    PaymentDisposition.TerminalSuccess -> Res.string.feature_payment_status_completed
    PaymentDisposition.TerminalFailure -> Res.string.feature_payment_status_failed
}
