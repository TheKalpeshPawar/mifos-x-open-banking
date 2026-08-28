/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentsstandingorder

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import template.core.base.designsystem.theme.KptTheme

/**
 * How wide the form is allowed to get.
 *
 * The same composition runs on a desktop window several times a phone's width, where a form stretched
 * edge to edge puts a 40-character reference field beside a 56dp avatar and reads as a spreadsheet.
 * `core/ui` carries no shared constraint to reuse — the only `widthIn` in it is the navigation rail's
 * minimum — so this is declared here rather than invented as a design-system component for one caller.
 */
internal val FormMaxWidth = 480.dp

/** An uppercase section label. Shared for the same reason as the spacing above. */
@Composable
internal fun SectionHeading(text: String) {
    Text(
        text = text,
        style = KptTheme.typography.titleMedium,
        color = KptTheme.colorScheme.onSurface,
    )
}
