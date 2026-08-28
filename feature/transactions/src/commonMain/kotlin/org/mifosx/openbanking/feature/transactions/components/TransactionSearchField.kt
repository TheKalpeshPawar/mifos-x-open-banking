/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.feature.transactions.TransactionsTestTags
import org.mifosx.openbanking.feature.transactions.generated.resources.Res
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_search_clear
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_search_hint
import template.core.base.designsystem.theme.KptTheme

/** Search field filtering the accumulated list by merchant / reference, with a clear affordance. */
@Composable
internal fun TransactionSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.xs)
            .testTag(TransactionsTestTags.SEARCH_FIELD),
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = KptTheme.colorScheme.onSurfaceVariant,
            )
        },
        placeholder = {
            Text(
                text = stringResource(Res.string.feature_transactions_search_hint),
                style = KptTheme.typography.bodyLarge,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        },
        textStyle = KptTheme.typography.bodyLarge,
        shape = DesignToken.shapes.pill,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = KptTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = KptTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = KptTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.testTag(TransactionsTestTags.SEARCH_CLEAR),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.feature_transactions_search_clear),
                    )
                }
            }
        },
    )
}
