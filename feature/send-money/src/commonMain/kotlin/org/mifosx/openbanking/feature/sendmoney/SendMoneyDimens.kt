/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import template.core.base.designsystem.theme.KptTheme

/** Spacing shared by the form page and the review page, so the two cannot drift apart. */
internal val ScreenPadding = 16.dp
internal val SectionGap = 16.dp
internal val HeadingGap = 8.dp
internal val RowGap = 12.dp
internal val HeroGap = 4.dp
internal val ChipCorner = 20.dp
internal val ChipPaddingHorizontal = 10.dp
internal val ChipPaddingVertical = 6.dp
internal val ChipGap = 8.dp
internal val ChipAvatarSize = 24.dp
internal val NoticeCorner = 12.dp
internal val NoticePadding = 12.dp
internal val NoticeGap = 10.dp
internal val NoticeIconSize = 16.dp

/** An uppercase section label. Shared for the same reason as the spacing above. */
@Composable
internal fun SectionHeading(text: String) {
    Text(
        text = text.uppercase(),
        style = KptTheme.typography.bodySmall,
        color = KptTheme.colorScheme.outline,
    )
}
