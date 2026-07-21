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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.feature.settings.SettingsTestTags

private val RowHorizontalPadding = 16.dp
private val RowVerticalPadding = 14.dp
private val LeadingIconSize = 24.dp
private val TrailingIconSize = 20.dp
private val LeadingGap = 16.dp
private val LineGap = 2.dp
private val LeadingSlotWidth = 24.dp

/**
 * One settings row: an optional leading icon, a title over an optional subtitle, and an optional
 * trailing slot.
 *
 * [onClick] is null for a row that only reports a value, which is what keeps the App Version row
 * honest — a row with no destination gets no ripple, no click target and no trailing affordance,
 * so it cannot look tappable while doing nothing.
 *
 * The row carries [testTag]; its title carries [SettingsTestTags.ROW], on a separate node because
 * a second `testTag` on one node replaces the first. The generic tag is what lets a suite count
 * rows without naming each one.
 */
@Composable
internal fun SettingsRow(
    title: String,
    testTag: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    subtitleTestTag: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
            .testTag(testTag)
            .padding(horizontal = RowHorizontalPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LeadingGap),
    ) {
        RowLeadingIcon(icon = icon)
        RowText(
            title = title,
            subtitle = subtitle,
            subtitleTestTag = subtitleTestTag,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke(this)
    }
}

/**
 * Rows with a leading icon and rows without it share one left edge for their text.
 *
 * The About group mixes both — Terms carries an icon, App Version does not — and letting the
 * unadorned row start further left would read as a different kind of row rather than the same one
 * without a glyph.
 */
@Composable
private fun RowLeadingIcon(icon: ImageVector?, modifier: Modifier = Modifier) {
    if (icon == null) {
        Spacer(modifier = modifier.width(LeadingSlotWidth))
    } else {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(LeadingIconSize),
        )
    }
}

@Composable
private fun RowText(
    title: String,
    subtitle: String?,
    subtitleTestTag: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LineGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(SettingsTestTags.ROW),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = subtitleTestTag?.let { Modifier.testTag(it) } ?: Modifier,
            )
        }
    }
}

/** Trailing chevron: this row opens a destination inside the app. */
@Composable
internal fun SettingsRowChevron(
    description: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .size(TrailingIconSize)
            .testTag(testTag),
    )
}

/** Trailing external-link glyph: this row leaves the app for a browser. */
@Composable
internal fun SettingsRowExternalLink(
    description: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .size(TrailingIconSize)
            .testTag(testTag),
    )
}
