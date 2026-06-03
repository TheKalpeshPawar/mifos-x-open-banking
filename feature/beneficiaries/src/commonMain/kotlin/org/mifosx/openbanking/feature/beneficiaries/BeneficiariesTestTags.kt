/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

/** UI test tags for the Beneficiaries screen. */
object BeneficiariesTestTags {
    const val SEARCH_BAR = "beneficiary_search_bar"
    const val RECENTLY_USED_HEADER = "recently_used_header"
    const val ALL_HEADER = "all_beneficiaries_header"
    const val SORT_BUTTON = "sort_button"
    const val ADD_FAB = "add_beneficiary_fab"
    const val EMPTY_STATE = "beneficiaries_empty_state"
    const val ERROR_STATE = "beneficiaries_error_state"
    const val ADD_SHEET = "add_beneficiary_sheet"

    fun beneficiaryRow(id: String) = "beneficiary_row_$id"
}
