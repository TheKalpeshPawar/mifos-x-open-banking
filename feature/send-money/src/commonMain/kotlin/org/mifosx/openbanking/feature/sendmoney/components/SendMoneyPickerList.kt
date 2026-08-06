/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.account.accountDisplayName
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_balance_available
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAccountRow
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyPickerRow

private val ListShape = RoundedCornerShape(12.dp)
private val RowMinHeight = 72.dp
private val RowPaddingHorizontal = 16.dp
private val RowPaddingVertical = 12.dp
private val RowGap = 16.dp
private val AvatarSize = 40.dp
private val TickSize = 20.dp
private val TextGap = 2.dp

/**
 * A grouped, single-select list — the shape both the payer and payee pickers take.
 *
 * One surface with hairline dividers rather than separate cards: these are alternatives within one
 * choice, and separating them into cards would read as several unrelated things.
 */
/**
 * The payer picker.
 *
 * Resolves each account's readable name here rather than taking a finished string, because HSBC
 * leaves `Nickname` blank on most accounts and the fallback — "Current account ·· 3349" — comes from
 * `core/ui`'s `@Composable` [accountDisplayName]. Reusing it is what keeps this list saying the same
 * thing as Home and Accounts; deriving a name locally is how the rows ended up blank.
 */
@Composable
internal fun SendMoneyAccountPickerList(
    rows: List<SendMoneyAccountRow>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    tagFor: (String) -> String,
    selectedLabel: String,
    modifier: Modifier = Modifier,
) {
    val resolved = rows.map { row ->
        val headline = accountDisplayName(
            nickname = row.nickname,
            accountSubType = row.accountSubType,
            accountNumber = row.accountNumber,
            rawIdentification = row.rawIdentification,
        )
        SendMoneyPickerRow(
            id = row.id,
            initials = initialsOf(headline),
            headline = headline,
            supporting = row.supporting.takeIf { it.isBlank() }
                ?: stringResource(Res.string.feature_send_money_balance_available, row.supporting),
        )
    }

    SendMoneyPickerList(
        rows = resolved,
        selectedId = selectedId,
        onSelect = onSelect,
        tagFor = tagFor,
        selectedLabel = selectedLabel,
        modifier = modifier,
    )
}

/**
 * Up to two letters from the name, for the row avatar.
 *
 * The trailing identifier is dropped first — an account reads "Current account ·· 3349", and
 * including the digits would render "C3" where the design asks for "CA". A single-word name falls
 * back to its first two letters so "Savings" gives "SA" rather than one lonely letter.
 */
internal fun initialsOf(name: String): String {
    val words = name.substringBefore('·')
        .substringBefore('•')
        .trim()
        .split(' ')
        .filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words.first().take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

@Composable
internal fun SendMoneyPickerList(
    rows: List<SendMoneyPickerRow>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    tagFor: (String) -> String,
    selectedLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ListShape)
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            SendMoneyPickerRowItem(
                row = row,
                selected = row.id == selectedId,
                selectedLabel = selectedLabel,
                onClick = { onSelect(row.id) },
                modifier = Modifier.testTag(tagFor(row.id)),
            )
        }
    }
}

/**
 * The tick keeps its space whether or not it is shown, so selecting a row cannot reflow the list
 * under the finger that is tapping it.
 */
@Composable
private fun SendMoneyPickerRowItem(
    row: SendMoneyPickerRow,
    selected: Boolean,
    selectedLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val onContainer = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container)
            .clickable(onClick = onClick)
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowPaddingHorizontal, vertical = RowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .background(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = row.initials,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TextGap),
        ) {
            Text(
                text = row.headline,
                style = MaterialTheme.typography.bodyLarge,
                color = onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.supporting,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = if (selected) selectedLabel else null,
            tint = onContainer,
            modifier = Modifier
                .size(TickSize)
                .alpha(if (selected) 1f else 0f),
        )
    }
}
