/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.settings.SettingsTestTags

private val HeaderHorizontalPadding = 16.dp
private val HeaderTopPadding = 20.dp
private val HeaderBottomPadding = 8.dp

/**
 * One titled group of settings rows: a tinted header band above a surface holding the rows.
 *
 * The header is coloured with the primary role rather than rendered as plain body text — it is the
 * only landmark separating groups on a screen that is otherwise an undifferentiated column of
 * rows, and a neutral header would leave the boundaries invisible.
 *
 * The group carries [testTag] and its header band carries [SettingsTestTags.SECTION], on separate
 * nodes because a second `testTag` on one node replaces the first rather than adding to it. The
 * generic tag is what lets a suite count sections without enumerating them.
 */
@Composable
internal fun SettingsSection(
    title: String,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SettingsTestTags.SECTION),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(
                        start = HeaderHorizontalPadding,
                        end = HeaderHorizontalPadding,
                        top = HeaderTopPadding,
                        bottom = HeaderBottomPadding,
                    )
                    .testTag(SettingsTestTags.SECTION_TITLE),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
