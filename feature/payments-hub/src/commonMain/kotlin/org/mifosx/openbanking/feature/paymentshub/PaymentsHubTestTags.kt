/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentshub

internal object PaymentsHubTestTags {
    const val SKELETON = "paymentsHub:skeleton"
    const val ACCOUNT_CHIPS = "paymentsHub:accountChips"
    const val QUICK_ACTIONS_GRID = "paymentsHub:quickActionsGrid"
    const val QUICK_ACTION_SEND_MONEY = "paymentsHub:quickActionSendMoney"
    const val QUICK_ACTION_SCHEDULE = "paymentsHub:quickActionSchedule"
    const val QUICK_ACTION_STANDING_ORDER = "paymentsHub:quickActionStandingOrder"
    const val QUICK_ACTION_INTERNATIONAL = "paymentsHub:quickActionInternational"
    const val ACTIVITY_LIST = "paymentsHub:activityList"
    const val EMPTY_ACTIVITY = "paymentsHub:emptyActivity"
    const val ERROR_SCREEN = "paymentsHub:errorScreen"
    const val RETRY_BUTTON = "paymentsHub:retryButton"

    fun accountChip(accountId: String): String = "paymentsHub:accountChip:$accountId"
    fun activityCard(id: String): String = "paymentsHub:activityCard:$id"
}
