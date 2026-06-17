/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.transactions

object TransactionTagsTestTags {
    const val HEADER_CARD = "transaction_tags_header_card"
    const val TAG_INPUT = "transaction_tags_input"
    const val ADD_BUTTON = "transaction_tags_add_button"
    const val NOTES_FIELD = "transaction_tags_notes_field"
    const val RECEIPT_AREA = "transaction_tags_receipt_area"
    const val CAMERA_BUTTON = "transaction_tags_camera_button"
    const val SAVE_BUTTON = "transaction_tags_save_button"
    fun chip(id: String) = "transaction_tags_chip_$id"
    fun suggestion(value: String) = "transaction_tags_suggestion_$value"
}
