/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.common

private const val UK_SORT_CODE_LENGTH = 6
private const val SORT_CODE_GROUP = 2

/**
 * Formats a six-digit UK sort code into the conventional dash-grouped form, e.g. `"400515"` →
 * `"40-05-15"`. Inputs that are not exactly six digits are returned unchanged.
 */
fun formatSortCode(sortCode: String): String =
    if (sortCode.length == UK_SORT_CODE_LENGTH) {
        sortCode.chunked(SORT_CODE_GROUP).joinToString("-")
    } else {
        sortCode
    }
