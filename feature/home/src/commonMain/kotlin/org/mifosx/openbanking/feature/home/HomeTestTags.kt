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

/** Stable `testTag` values for the home screen, shared by every home test suite. */
internal object HomeTestTags {
    const val CONTENT = "home_content"
    const val HEADER = "home_header"
    const val GREETING = "home_greeting"
    const val AVATAR = "home_avatar"
    const val QUICK_ACTIONS = "home_quick_actions"
    const val CARDS_SECTION = "home_cards_section"
    const val ACCOUNTS_SECTION = "home_accounts_section"

    private const val QUICK_ACTION_PREFIX = "home_quick_action_"
    private const val CARD_FACE_PREFIX = "home_card_face_"
    private const val ACCOUNT_ROW_PREFIX = "home_account_row_"

    fun quickAction(key: String): String = QUICK_ACTION_PREFIX + key

    fun cardFace(accountId: String): String = CARD_FACE_PREFIX + accountId

    fun accountRow(accountId: String): String = ACCOUNT_ROW_PREFIX + accountId
}
