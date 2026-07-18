/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts

import org.mifosx.openbanking.feature.accounts.ui.AccountFilter

/**
 * Stable Compose `testTag` identifiers for the accounts screen. Shared by the Robolectric and
 * on-device instrumented UI tests so both drive the screen through the same vocabulary.
 */
internal object AccountsTestTags {
    const val CONTENT = "accounts_content"
    const val TOTAL_SUMMARY = "accounts_total_summary"
    const val FILTER_ROW = "accounts_filter_row"
    const val SKELETON = "accounts_skeleton"
    const val EMPTY = "accounts_empty"
    const val MANAGE_CONSENTS = "accounts_manage_consents"
    const val ERROR = "accounts_error"
    const val ERROR_RETRY = "accounts_error_retry"
    const val CONSENT_BANNER = "accounts_consent_banner"
    const val CONSENT_RECONFIRM = "accounts_consent_reconfirm"
    const val BALANCE_OWED_BADGE = "accounts_balance_owed_badge"

    private const val ACCOUNT_CARD_PREFIX = "accounts_card_"
    private const val FILTER_CHIP_PREFIX = "accounts_filter_"

    /** Per-row tag for an account card, e.g. `accounts_card_acc-1`. */
    fun accountCard(accountId: String): String = ACCOUNT_CARD_PREFIX + accountId

    /** Per-chip tag for a filter, e.g. `accounts_filter_current`. */
    fun filterChip(filter: AccountFilter): String = FILTER_CHIP_PREFIX + filter.name.lowercase()
}
