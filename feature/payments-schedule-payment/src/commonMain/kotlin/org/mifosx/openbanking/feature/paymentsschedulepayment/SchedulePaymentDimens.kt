/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentsschedulepayment

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import template.core.base.designsystem.theme.KptTheme

internal val FormMaxWidth = 480.dp

/** An uppercase section label. Shared for the same reason as the spacing above. */
@Composable
internal fun SectionHeading(text: String) {
    Text(
        text = text,
        style = KptTheme.typography.titleMedium,
        color = KptTheme.colorScheme.primary,
    )
}
