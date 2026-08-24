/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail

/**
 * Stable `testTag` values for the account-detail screen, shared by the Robolectric and
 * instrumented suites so both drive the same nodes.
 *
 * Append-only: a tag that an existing test references must not be renamed or removed without a
 * matching `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object AccountDetailTestTags {

    const val LOADING_SPINNER = "accountDetail:loadingSpinner"
    const val LOADING_SKELETON = "accountDetail:loadingSkeleton"

    const val HEADER_CARD = "accountDetail:headerCard"
    const val DESCRIPTION_CARD = "accountDetail:descriptionCard"
    const val SUBTYPE_LABEL = "accountDetail:subtypeLabel"
    const val DISPLAY_NAME = "accountDetail:displayName"
    const val IDENTIFICATION = "accountDetail:identification"
    const val CURRENCY_BADGE = "accountDetail:currencyBadge"
    const val SERVICER_BADGE = "accountDetail:servicerBadge"
    const val LAST_UPDATED = "accountDetail:lastUpdated"

    const val BALANCES_HEADER = "accountDetail:balancesHeader"
    const val BALANCES_LIST = "accountDetail:balancesList"
    const val BALANCES_EMPTY_STATE = "accountDetail:balancesEmptyState"

    const val EXPLORE_HEADER = "accountDetail:exploreHeader"
    const val CHIP_ROW = "accountDetail:chipRow"

    const val ERROR_STATE = "accountDetail:errorState"
    const val ERROR_TITLE = "accountDetail:errorTitle"
    const val ERROR_BODY = "accountDetail:errorBody"
    const val RETRY_BUTTON = "accountDetail:retryButton"

    /** Tag for one typed balance row, keyed by its OBIE `Type`. */
    fun balanceRow(type: String): String = "accountDetail:balanceRow:$type"

    /** Tag for one Explore chip, keyed by its destination. */
    fun chip(destination: AccountDetailChip): String = "accountDetail:chip:${destination.name}"
}
