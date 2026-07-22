/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.statements.generated.resources.Res
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_closing_balance_label
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_date_range
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_download_accessibility
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_download_in_progress_accessibility
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_list_accessibility
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_row_accessibility
import org.mifosx.openbanking.feature.statements.ui.DownloadState
import org.mifosx.openbanking.feature.statements.ui.StatementRowUiModel

private val RowMinHeight = 72.dp
private val RowHorizontalPadding = 16.dp
private val RowEndPadding = 8.dp
private val RowVerticalPadding = 12.dp
private val RowTextGap = 2.dp
private val RowTrailingGap = 4.dp
private val DownloadButtonSize = 40.dp
private val SpinnerSize = 20.dp
private val SpinnerStroke = 2.dp

/**
 * The statements body: a lazy list of period rows, a hairline divider between each.
 *
 * The list is lazy because the number of statements an account has is bank-decided and unbounded —
 * OBIE returns the full set in one unpaginated response, so the composition, not the request, is
 * where this is kept cheap.
 */
@Composable
internal fun StatementsContent(
    statements: List<StatementRowUiModel>,
    downloadState: Map<String, DownloadState>,
    onRowClick: (String) -> Unit,
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listDescription = stringResource(Res.string.feature_statements_list_accessibility)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(StatementsTestTags.CONTENT_LIST)
            .semantics { contentDescription = listDescription },
    ) {
        itemsIndexed(
            items = statements,
            key = { _, row -> row.statementId.ifBlank { row.statementReference } },
        ) { index, row ->
            StatementRow(
                row = row,
                isDownloading = downloadState[row.statementId] == DownloadState.InProgress,
                onRowClick = onRowClick,
                onDownload = onDownload,
            )
            if (index < statements.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = RowHorizontalPadding),
                )
            }
        }
    }
}

/**
 * One statement period. The whole row navigates to the detail screen; the trailing download button is
 * its own click target, so tapping it downloads the file without also opening the detail.
 */
@Composable
private fun StatementRow(
    row: StatementRowUiModel,
    isDownloading: Boolean,
    onRowClick: (String) -> Unit,
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateRange = stringResource(
        Res.string.feature_statements_date_range,
        row.startDateFormatted,
        row.endDateFormatted,
    )
    val rowDescription = stringResource(
        Res.string.feature_statements_row_accessibility,
        row.statementReference,
        dateRange,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .clickable { onRowClick(row.statementId) }
            .padding(
                start = RowHorizontalPadding,
                end = RowEndPadding,
                top = RowVerticalPadding,
                bottom = RowVerticalPadding,
            )
            .testTag(StatementsTestTags.row(row.statementId))
            .semantics { contentDescription = rowDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatementRowText(row = row, modifier = Modifier.weight(1f))
        StatementRowTrailing(
            row = row,
            dateRange = dateRange,
            isDownloading = isDownloading,
            onDownload = onDownload,
        )
    }
}

/**
 * The leading text block: the period headline and the closing-balance supporting line, whose amount
 * is monospaced so figures across rows align on the decimal point.
 */
@Composable
private fun StatementRowText(row: StatementRowUiModel, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.feature_statements_closing_balance_label)
    val supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RowTextGap)) {
        Text(
            text = row.periodLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = supportingColor)) { append("$label ") }
                withStyle(SpanStyle(color = supportingColor, fontFamily = FontFamily.Monospace)) {
                    append(row.closingBalanceFormatted)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** The trailing column: the period date range above the download control, both right-aligned. */
@Composable
private fun StatementRowTrailing(
    row: StatementRowUiModel,
    dateRange: String,
    isDownloading: Boolean,
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(RowTrailingGap),
    ) {
        Text(
            text = dateRange,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        DownloadControl(
            statementId = row.statementId,
            statementReference = row.statementReference,
            isDownloading = isDownloading,
            onDownload = onDownload,
        )
    }
}

/**
 * The download affordance for one row: a 40dp round icon button that tints to primary on hover, swapped
 * for a same-footprint spinner while that statement's download is in flight.
 */
@Composable
private fun DownloadControl(
    statementId: String,
    statementReference: String,
    isDownloading: Boolean,
    onDownload: (String) -> Unit,
) {
    val downloadDescription = stringResource(
        Res.string.feature_statements_download_accessibility,
        statementReference,
    )
    val spinnerDescription =
        stringResource(Res.string.feature_statements_download_in_progress_accessibility)

    if (isDownloading) {
        Box(
            modifier = Modifier
                .size(DownloadButtonSize)
                .testTag(StatementsTestTags.downloadSpinner(statementId))
                .semantics { contentDescription = spinnerDescription },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpinnerSize),
                strokeWidth = SpinnerStroke,
            )
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        IconButton(
            onClick = { onDownload(statementId) },
            interactionSource = interactionSource,
            modifier = Modifier
                .size(DownloadButtonSize)
                .testTag(StatementsTestTags.downloadButton(statementId)),
        ) {
            Icon(
                imageVector = Icons.Filled.FileDownload,
                contentDescription = downloadDescription,
                tint = if (isHovered) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
