/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders

object StandingOrderDetailTestTags {
    const val HEADER = "standing_order_detail_header"
    const val STATUS_BADGE = "standing_order_detail_status_badge"
    const val RECIPIENT_CARD = "standing_order_detail_recipient_card"
    const val SCHEDULE_CARD = "standing_order_detail_schedule_card"
    const val AMOUNT_CARD = "standing_order_detail_amount_card"
    const val HISTORY_CARD = "standing_order_detail_history_card"
    const val PAUSE_BUTTON = "standing_order_detail_pause_button"
    const val CANCEL_BUTTON = "standing_order_detail_cancel_button"
    fun executionRow(transactionId: String) = "standing_order_detail_execution_$transactionId"
}
