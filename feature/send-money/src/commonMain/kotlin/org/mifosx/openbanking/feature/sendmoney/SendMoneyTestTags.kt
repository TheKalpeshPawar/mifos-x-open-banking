/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

/** UI test tags for the Send Money + Confirm Payment screens. */
object SendMoneyTestTags {
    const val FROM_ACCOUNT = "from_account_selector"
    const val AMOUNT_INPUT = "amount_input"
    const val BENEFICIARY_SEARCH = "beneficiary_search"
    const val REFERENCE_INPUT = "reference_input"
    const val FEE_BANNER = "fee_estimate_banner"
    const val CONTINUE_BUTTON = "continue_button"

    const val CONFIRM_AMOUNT = "payment_amount"
    const val CONFIRM_TO = "to_value"
    const val CONFIRM_TOTAL = "total_value"
    const val CONFIRM_SEND_BUTTON = "confirm_send_button"
    const val EDIT_PAYMENT_BUTTON = "edit_payment_button"

    fun beneficiaryChip(id: String) = "beneficiary_chip_$id"
    fun paymentType(type: String) = "payment_type_$type"
}
