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

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
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
import template.core.base.designsystem.theme.KptTheme

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
            start = KptTheme.spacing.md,
            end = KptTheme.spacing.md,
            bottom = DesignToken.sizes.badge,
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
    Surface(
        color = KptTheme.colorScheme.surfaceContainer,
        shape = KptTheme.shapes.medium,
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag(StatementDetailTestTags.HEADER_CARD)
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = statement.reference,
                style = KptTheme.typography.labelMedium,
                color = KptTheme.colorScheme.secondary,
            )
            Text(
                text = statement.periodLabel,
                style = KptTheme.typography.titleMedium,
                color = KptTheme.colorScheme.onSurface,
            )
            Text(
                text = statement.type,
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    Res.string.feature_statement_detail_created,
                    statement.createdDateLabel,
                ),
                style = KptTheme.typography.labelSmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
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
        style = KptTheme.typography.titleSmall,
        color = KptTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = KptTheme.spacing.lg, bottom = KptTheme.spacing.sm)
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
                HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
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
            .heightIn(min = DesignToken.sizes.cardRow)
            .padding(vertical = KptTheme.spacing.sm)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = line.label,
            style = KptTheme.typography.bodyLarge,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = line.amountFormatted,
            style = KptTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = line.color.toAmountColor(),
            modifier = Modifier.padding(start = KptTheme.spacing.md),
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
            .heightIn(min = DesignToken.sizes.cardRow)
            .clickable { onRowClick(row.transactionId, row.accountId) }
            .padding(vertical = KptTheme.spacing.sm)
            .testTag(StatementDetailTestTags.txnRow(row.transactionId))
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = row.dateLabel,
                style = KptTheme.typography.labelSmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = row.info,
                style = KptTheme.typography.bodyLarge,
                color = KptTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Text(
            text = row.amountFormatted,
            style = KptTheme.typography.titleSmall,
            fontFamily = FontFamily.Monospace,
            color = if (row.isCredit) {
                KptTheme.colorScheme.tertiary
            } else {
                KptTheme.colorScheme.error
            },
            modifier = Modifier.padding(start = KptTheme.spacing.md),
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
    Column(modifier = modifier.fillMaxWidth().padding(top = KptTheme.spacing.lg)) {
        OutlinedButton(
            onClick = onDownload,
            enabled = !isDownloading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DesignToken.sizes.iconExtraLarge)
                .testTag(StatementDetailTestTags.DOWNLOAD_BUTTON)
                .semantics { contentDescription = downloadDescription },
        ) {
            Icon(
                imageVector = Icons.Filled.FileDownload,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = KptTheme.spacing.sm)
                    .size(DesignToken.sizes.iconExtraSmall),
            )
            Text(text = stringResource(Res.string.feature_statement_detail_action_download_pdf))
        }
        if (isDownloading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = KptTheme.spacing.sm)
                    .testTag(StatementDetailTestTags.DOWNLOAD_PROGRESS)
                    .semantics { contentDescription = progressDescription },
            )
        }
    }
}

@Composable
private fun StatementAmountColor.toAmountColor(): Color = when (this) {
    StatementAmountColor.Credit -> KptTheme.colorScheme.tertiary
    StatementAmountColor.Debit -> KptTheme.colorScheme.error
    StatementAmountColor.Neutral -> KptTheme.colorScheme.onSurface
}
