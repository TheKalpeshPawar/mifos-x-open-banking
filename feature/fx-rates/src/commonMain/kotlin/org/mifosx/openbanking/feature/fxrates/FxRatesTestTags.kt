/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.fxrates

/** Stable test tags for the Exchange Rates screen (ids mirror the idea-layer spec). */
object FxRatesTestTags {
    const val TITLE = "fx_rates_title"
    const val LAST_UPDATED = "last_updated_text"
    const val CONVERTER_CARD = "converter_card"
    const val SEND_AMOUNT_INPUT = "send_amount_input"
    const val FROM_CURRENCY_SELECT = "from_currency_select"
    const val SWAP_CURRENCIES = "swap_currencies_icon"
    const val TO_CURRENCY_SELECT = "to_currency_select"
    const val CONVERTED_RESULT = "converted_result_display"
    const val RATE_INFO = "rate_info_text"
    const val SEND_MONEY_CTA = "send_money_cta"
    const val POPULAR_PAIRS_HEADER = "popular_pairs_header"

    fun pairRow(from: String, to: String) = "rate_row_${from.lowercase()}_${to.lowercase()}"
}
