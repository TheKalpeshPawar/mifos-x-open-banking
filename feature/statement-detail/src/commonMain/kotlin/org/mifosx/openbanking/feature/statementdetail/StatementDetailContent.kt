/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.statementdetail.generated.resources.Res
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_action_download_pdf
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_created
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_download_pdf_a11y
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_downloading
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_header_card_a11y
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_line_a11y
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_section_balances
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_section_fees
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_section_interest
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_section_transactions
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_transaction_row_a11y
import org.mifosx.openbanking.feature.statementdetail.ui.DownloadState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementAmountColor
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailUiModel
import org.mifosx.openbanking.feature.statementdetail.ui.StatementLineUiModel
import org.mifosx.openbanking.feature.statementdetail.ui.StatementTxnRowUiModel

private val ScreenPadding = 16.dp
private val ListBottomPadding = 100.dp
private val CardCornerRadius = 12.dp
private val CardElevation = 2.dp
private val HeaderInnerPadding = 16.dp
private val HeaderLineGap = 4.dp
private val SectionHeaderTopPadding = 20.dp
private val SectionHeaderBottomPadding = 8.dp
private val RowMinHeight = 52.dp
private val RowVerticalPadding = 10.dp
private val RowTrailingGap = 12.dp
private val TxnTextGap = 2.dp
private val DownloadTopPadding = 24.dp
private val DownloadButtonHeight = 40.dp
private val DownloadIconSize = 18.dp
private val DownloadIconGap = 8.dp
private val ProgressTopPadding = 10.dp

/**
 * The statement-detail content body: the period header card, the money sections (Fees and Interest
 * shown only when present), the tappable transactions list, and the Download PDF control.
 *
 * A [LazyColumn] because a statement period can hold an unbounded number of transactions.
 */
@Composable
internal fun StatementDetailContent(
    statement: StatementDetailUiModel,
    transactions: List<StatementTxnRowUiModel>,
    downloadState: DownloadState,
    onRowClick: (transactionId: String, accountId: String) -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(StatementDetailTestTags.CONTENT_LIST),
        contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            bottom = ListBottomPadding,
        ),
    ) {
        statementSections(statement)
        item(key = "transactions-header") {
            StatementSectionHeader(
                labelRes = Res.string.feature_statement_detail_section_transactions,
                sectionTag = StatementDetailTestTags.TRANSACTIONS_SECTION,
            )
        }
        items(
            items = transactions,
            key = { row -> row.transactionId.ifBlank { row.info } },
        ) { row ->
            StatementTransactionRow(row = row, onRowClick = onRowClick)
        }
        item(key = "download") {
            StatementDownloadSection(
                isDownloading = downloadState == DownloadState.Downloading,
                onDownload = onDownload,
            )
        }
    }
}

/**
 * The header card and the three money sections shared by the content and empty states. Fees and
 * Interest are omitted when their lists are empty, matching the design's `visible_when` rules.
 */
internal fun LazyListScope.statementSections(statement: StatementDetailUiModel) {
    item(key = "header") { StatementHeaderCard(statement) }

    item(key = "balances") {
        StatementLineSection(
            labelRes = Res.string.feature_statement_detail_section_balances,
            lines = statement.balances,
            sectionTag = StatementDetailTestTags.BALANCES_SECTION,
        )
    }

    if (statement.fees.isNotEmpty()) {
        item(key = "fees") {
            StatementLineSection(
                labelRes = Res.string.feature_statement_detail_section_fees,
                lines = statement.fees,
                sectionTag = StatementDetailTestTags.FEES_SECTION,
            )
        }
    }

    if (statement.interest.isNotEmpty()) {
        item(key = "interest") {
            StatementLineSection(
                labelRes = Res.string.feature_statement_detail_section_interest,
                lines = statement.interest,
                sectionTag = StatementDetailTestTags.INTEREST_SECTION,
            )
        }
    }
}

@Composable
private fun StatementHeaderCard(statement: StatementDetailUiModel, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_statement_detail_header_card_a11y)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
        shape = RoundedCornerShape(CardCornerRadius),
        modifier = modifier
            .fillMaxWidth()
            .testTag(StatementDetailTestTags.HEADER_CARD)
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(HeaderInnerPadding),
            verticalArrangement = Arrangement.spacedBy(HeaderLineGap),
        ) {
            Text(
                text = statement.reference,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = statement.periodLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = statement.type,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    Res.string.feature_statement_detail_created,
                    statement.createdDateLabel,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun StatementSectionHeader(
    labelRes: StringResource,
    sectionTag: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SectionHeaderTopPadding, bottom = SectionHeaderBottomPadding)
            .testTag(sectionTag),
    )
}

@Composable
private fun StatementLineSection(
    labelRes: StringResource,
    lines: List<StatementLineUiModel>,
    sectionTag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StatementSectionHeader(labelRes = labelRes, sectionTag = sectionTag)
        lines.forEachIndexed { index, line ->
            StatementLineRow(line = line)
            if (index < lines.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun StatementLineRow(line: StatementLineUiModel, modifier: Modifier = Modifier) {
    val description = stringResource(
        Res.string.feature_statement_detail_line_a11y,
        line.label,
        line.amountFormatted,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(vertical = RowVerticalPadding)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = line.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = line.amountFormatted,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = line.color.toAmountColor(),
            modifier = Modifier.padding(start = RowTrailingGap),
        )
    }
}

@Composable
private fun StatementTransactionRow(
    row: StatementTxnRowUiModel,
    onRowClick: (transactionId: String, accountId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        Res.string.feature_statement_detail_transaction_row_a11y,
        row.info,
        row.amountFormatted,
        row.dateLabel,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .clickable { onRowClick(row.transactionId, row.accountId) }
            .padding(vertical = RowVerticalPadding)
            .testTag(StatementDetailTestTags.txnRow(row.transactionId))
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TxnTextGap),
        ) {
            Text(
                text = row.dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = row.info,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Text(
            text = row.amountFormatted,
            style = MaterialTheme.typography.titleSmall,
            fontFamily = FontFamily.Monospace,
            color = if (row.isCredit) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.padding(start = RowTrailingGap),
        )
    }
}

/**
 * The Download PDF control: a full-width outlined button (disabled while downloading) above an
 * indeterminate linear progress bar that shows only while the download is in flight.
 */
@Composable
internal fun StatementDownloadSection(
    isDownloading: Boolean,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloadDescription = stringResource(Res.string.feature_statement_detail_download_pdf_a11y)
    val progressDescription = stringResource(Res.string.feature_statement_detail_downloading)
    Column(modifier = modifier.fillMaxWidth().padding(top = DownloadTopPadding)) {
        OutlinedButton(
            onClick = onDownload,
            enabled = !isDownloading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DownloadButtonHeight)
                .testTag(StatementDetailTestTags.DOWNLOAD_BUTTON)
                .semantics { contentDescription = downloadDescription },
        ) {
            Icon(
                imageVector = Icons.Filled.FileDownload,
                contentDescription = null,
                modifier = Modifier.padding(end = DownloadIconGap).size(DownloadIconSize),
            )
            Text(text = stringResource(Res.string.feature_statement_detail_action_download_pdf))
        }
        if (isDownloading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ProgressTopPadding)
                    .testTag(StatementDetailTestTags.DOWNLOAD_PROGRESS)
                    .semantics { contentDescription = progressDescription },
            )
        }
    }
}

@Composable
private fun StatementAmountColor.toAmountColor(): Color = when (this) {
    StatementAmountColor.Credit -> MaterialTheme.colorScheme.tertiary
    StatementAmountColor.Debit -> MaterialTheme.colorScheme.error
    StatementAmountColor.Neutral -> MaterialTheme.colorScheme.onSurface
}
