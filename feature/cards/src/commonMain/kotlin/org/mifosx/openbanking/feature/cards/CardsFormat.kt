/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.cards

/**
 * Pure presentation helpers for the My Cards screen. Kept out of the Composable file so the
 * screen stays focused on layout (and under Detekt's per-file function budget), mirroring the
 * `HomeFormat` convention — every function here is deterministic and unit-testable.
 */

/** Masks all but the last four digits of an OBP card number for display. */
internal fun maskedNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    val last4 = digits.takeLast(4)
    return if (last4.isBlank()) raw else "•••• •••• •••• $last4"
}
