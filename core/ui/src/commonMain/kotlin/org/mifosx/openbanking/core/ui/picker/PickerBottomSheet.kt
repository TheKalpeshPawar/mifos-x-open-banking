/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package org.mifosx.openbanking.core.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable single-choice picker bottom sheet — the shared surface behind the account
 * picker on Home and the bank picker on Products. Lists [options] with a divider between
 * rows (never under the last row, where a clipped divider reads as a stray line); the row
 * whose id matches [selectedId] carries a check. Selecting a row reports the option and
 * the caller dismisses; swiping down / tapping outside calls [onDismiss]. The list is a
 * [LazyColumn] so long option lists scroll instead of clipping mid-row at the sheet fold.
 */
@Composable
fun PickerBottomSheet(
    options: List<PickerSheetOption>,
    selectedId: String,
    onOptionSelected: (PickerSheetOption) -> Unit,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    sheetTestTag: String = "picker_sheet",
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.testTag(sheetTestTag),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(options, key = { _, option -> option.id }) { index, option ->
                Column {
                    PickerRow(
                        option = option,
                        selected = option.id == selectedId,
                        onClick = { onOptionSelected(option) },
                    )
                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(option: PickerSheetOption, selected: Boolean, onClick: () -> Unit) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 24.dp, vertical = 14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (option.testTag.isBlank()) rowModifier else rowModifier.testTag(option.testTag),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                option.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (option.supportingText.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    option.supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
