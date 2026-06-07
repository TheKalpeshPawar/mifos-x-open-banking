/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits

/** Stable test tags for the Direct Debits screen (ids mirror the idea-layer spec). */
object DirectDebitsTestTags {
    const val TITLE = "direct_debits_title"
    const val ACTIVE_COUNT_CHIP = "active_count_chip"
    const val CANCEL_DIALOG = "cancel_confirm_dialog"
    const val CANCEL_CONFIRM_CTA = "cancel_confirm_cta"
    const val CANCEL_DISMISS_CTA = "cancel_dismiss_cta"

    fun mandateCard(id: String) = "direct_debit_$id"

    fun cancelButton(id: String) = "cancel_direct_debit_$id"
}
