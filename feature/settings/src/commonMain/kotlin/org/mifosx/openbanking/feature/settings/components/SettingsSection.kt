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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.mifosx.openbanking.feature.settings.SettingsTestTags
import template.core.base.designsystem.theme.KptTheme

/**
 * One titled group of settings rows: a tinted header band above a surface holding the rows.
 *
 * @param testTag Carried by the group; its header band carries [SettingsTestTags.SECTION].
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
            color = KptTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SettingsTestTags.SECTION),
        ) {
            Text(
                text = title,
                style = KptTheme.typography.titleSmall,
                color = KptTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(
                        start = KptTheme.spacing.md,
                        end = KptTheme.spacing.md,
                        top = KptTheme.spacing.lg,
                        bottom = KptTheme.spacing.sm,
                    )
                    .testTag(SettingsTestTags.SECTION_TITLE),
            )
        }
        HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
        Surface(color = KptTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
        HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
    }
}
