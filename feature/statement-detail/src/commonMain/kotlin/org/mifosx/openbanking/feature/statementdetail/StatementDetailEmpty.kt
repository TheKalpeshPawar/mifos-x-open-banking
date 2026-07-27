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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
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
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.statementdetail.generated.resources.Res
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_empty_txns_a11y
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_empty_txns_body
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_empty_txns_title
import org.mifosx.openbanking.feature.statementdetail.ui.DownloadState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailUiModel

private val ScreenPadding = 16.dp
private val ListBottomPadding = 100.dp
private val IconWellSize = 72.dp
private val IconSize = 36.dp
private val BlockTopPadding = 24.dp
private val LineGap = 8.dp

/**
 * Empty state: the statement loaded but its period held no transactions. The header card, balances,
 * fees and interest still render (a valid OBIE edge case, not a fault); a `receipt_long` block replaces
 * the transactions list, and Download PDF remains available.
 */
@Composable
internal fun StatementDetailEmpty(
    statement: StatementDetailUiModel,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(StatementDetailTestTags.EMPTY_LIST),
        contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            bottom = ListBottomPadding,
        ),
    ) {
        statementSections(statement)
        item(key = "empty-transactions") { StatementEmptyTransactions() }
        item(key = "download") {
            StatementDownloadSection(
                isDownloading = downloadState == DownloadState.Downloading,
                onDownload = onDownload,
            )
        }
    }
}

@Composable
private fun StatementEmptyTransactions(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_statement_detail_empty_txns_a11y)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = BlockTopPadding)
            .testTag(StatementDetailTestTags.EMPTY_TRANSACTIONS)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconWellSize)
                .background(color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = stringResource(Res.string.feature_statement_detail_empty_txns_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(StatementDetailTestTags.EMPTY_TRANSACTIONS_TITLE),
        )
        Text(
            text = stringResource(Res.string.feature_statement_detail_empty_txns_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(StatementDetailTestTags.EMPTY_TRANSACTIONS_BODY),
        )
    }
}
