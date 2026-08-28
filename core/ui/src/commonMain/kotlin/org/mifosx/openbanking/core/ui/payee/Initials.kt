/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.payee

/**
 * Up to two letters from a name, for an avatar: the first letter of the first two words, or the
 * first two letters of a single-word name. Anything from an identifier marker onwards is ignored.
 */
fun initialsOf(name: String): String {
    val words = name.nameHalf()
        .split(' ')
        .filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words.first().take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/** The part of a label before any identifier marker. */
internal fun String.nameHalf(): String = substringBefore('·').substringBefore('•').trim()
