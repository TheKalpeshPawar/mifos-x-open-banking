/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.ui.components.EmptyDataComponent
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.Res
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_list_a11y
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_reference_a11y
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_row_a11y
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_scheme_account
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_scheme_card
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_scheme_iban
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_scheme_paym
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_scheme_sort_code
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_search_a11y
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_search_empty_title
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_search_placeholder
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesUiState
import org.mifosx.openbanking.feature.beneficiaries.ui.formatIdentification
import template.core.base.designsystem.theme.KptTheme

/**
 * Content state: the search field, then the payee list.
 *
 * The search field sits outside the scrolling list so it stays reachable while the rows scroll —
 * matching the design, where it is pinned under the app bar rather than being the list's first item.
 *
 * When a live query matches nothing the list is replaced by the no-results block, but the state
 * stays `Content`: the query is still live and the unfiltered list is still held, so clearing the
 * field brings every row straight back without a re-fetch.
 */
@Composable
internal fun BeneficiariesContent(
    content: BeneficiariesUiState.Content,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchField(query = content.query, onSearch = onSearch)
        HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)

        if (content.isSearchWithoutMatches) {
            EmptyDataComponent(
                isEmptyData = true,
                message = stringResource(Res.string.feature_beneficiaries_search_empty_title),
            )
        } else {
            BeneficiaryList(rows = content.filtered)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_beneficiaries_search_a11y)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(KptTheme.colorScheme.surface)
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.sm),
    ) {
        TextField(
            value = query,
            onValueChange = onSearch,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = KptTheme.colorScheme.onSurfaceVariant,
                )
            },
            placeholder = {
                Text(
                    text = stringResource(Res.string.feature_beneficiaries_search_placeholder),
                    style = KptTheme.typography.bodyLarge,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
            },
            textStyle = KptTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions.Default,
            shape = DesignToken.shapes.pill,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = KptTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = KptTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = KptTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DesignToken.sizes.cardRow)
                .testTag(BeneficiariesTestTags.SEARCH_FIELD)
                .semantics { contentDescription = description },
        )
    }
}

@Composable
private fun BeneficiaryList(
    rows: List<BeneficiaryItem>,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_beneficiaries_list_a11y)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(BeneficiariesTestTags.CONTENT_LIST)
            .semantics { contentDescription = description },
    ) {
        items(items = rows, key = { it.beneficiaryId }) { row ->
            BeneficiaryRow(row = row)
            if (row != rows.last()) {
                HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/**
 * One payee row.
 *
 * Deliberately not clickable: this is a read-only AISP list, there is no payee detail screen, and an
 * earlier accessibility label promising "tap to view payment details" was removed for exactly that
 * reason. The whole row carries a single merged description so a screen reader reads the payee once
 * rather than announcing four disconnected fragments.
 */
@Composable
private fun BeneficiaryRow(
    row: BeneficiaryItem,
    modifier: Modifier = Modifier,
) {
    val schemeLabel = stringResource(row.scheme.labelResource())
    val rowDescription = stringResource(
        Res.string.feature_beneficiaries_row_a11y,
        row.creditorName,
        schemeLabel,
        formatIdentification(row.identification, row.scheme),
    )
    val referenceDescription = stringResource(Res.string.feature_beneficiaries_reference_a11y, row.reference)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DesignToken.sizes.rowTall)
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.sm)
            .testTag(BeneficiariesTestTags.row(row.beneficiaryId))
            .semantics(mergeDescendants = true) { contentDescription = "$rowDescription, $referenceDescription" },
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.creditorName,
                style = KptTheme.typography.bodyLarge,
                color = KptTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SupportingLine(
                schemeLabel = schemeLabel,
                identification = formatIdentification(row.identification, row.scheme),
            )
        }

        Text(
            text = row.reference,
            style = KptTheme.typography.labelSmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = DesignToken.sizes.badge),
        )
    }
}

/**
 * The scheme label and the account identifier, separated by a middot.
 *
 * The identifier is monospaced so digit groups line up between rows — a column of sort codes is far
 * easier to scan when the glyphs are the same width.
 */
@Composable
private fun SupportingLine(
    schemeLabel: String,
    identification: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$schemeLabel ·",
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = identification,
            style = KptTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = KptTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun BeneficiaryScheme.labelResource(): StringResource = when (this) {
    BeneficiaryScheme.SortCode -> Res.string.feature_beneficiaries_scheme_sort_code
    BeneficiaryScheme.Iban -> Res.string.feature_beneficiaries_scheme_iban
    BeneficiaryScheme.Paym -> Res.string.feature_beneficiaries_scheme_paym
    BeneficiaryScheme.Card -> Res.string.feature_beneficiaries_scheme_card
    BeneficiaryScheme.Account -> Res.string.feature_beneficiaries_scheme_account
}
