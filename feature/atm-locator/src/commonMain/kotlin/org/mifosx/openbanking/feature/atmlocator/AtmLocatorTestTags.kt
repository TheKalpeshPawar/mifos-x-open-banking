/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.atmlocator

/** Stable test tags for the ATM Locator screen. */
object AtmLocatorTestTags {
    const val TITLE = "atm_locator_title"
    const val SEARCH = "atm_locator_search"
    const val NEAR_ME = "atm_locator_near_me"
    const val MAP = "atm_locator_map"
    const val RESULT_HEADER = "atm_locator_result_header"
    const val RETRY = "atm_locator_retry"

    fun atmRow(id: String) = "atm_row_$id"
}
