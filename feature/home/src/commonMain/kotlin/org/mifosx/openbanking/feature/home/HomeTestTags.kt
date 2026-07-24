/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home

/**
 * Stable Compose `testTag` identifiers for the home dashboard surfaces. Shared by the Robolectric
 * and on-device instrumented UI tests so both drive the screen through the same vocabulary.
 */
internal object HomeTestTags {
    const val CONTENT = "home_content"
    const val SKELETON = "home_skeleton"
    const val HERO_CARD = "home_hero_card"
    const val QUICK_ACTIONS = "home_quick_actions"
    const val RECENT_TRANSACTIONS = "home_recent_transactions"
    const val SPENDING_CARD = "home_spending_card"
    const val ERROR_RETRY = "home_error_retry"

    private const val ACCOUNT_CHIP_PREFIX = "home_account_chip_"

    /** Per-chip tag for the account switcher, e.g. `home_account_chip_acc-1`. */
    fun accountChip(accountId: String): String = ACCOUNT_CHIP_PREFIX + accountId
}
