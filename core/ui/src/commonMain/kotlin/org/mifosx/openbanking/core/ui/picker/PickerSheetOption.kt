/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.ui.picker

import androidx.compose.runtime.Immutable

/**
 * One selectable row in [PickerBottomSheet]. The [id] is the selection key — it must be
 * unique within a sheet's option list. A blank [supportingText] suppresses the second
 * line entirely; a blank [testTag] leaves the row untagged.
 */
@Immutable
data class PickerSheetOption(
    val id: String,
    val label: String,
    val supportingText: String = "",
    val testTag: String = "",
)
