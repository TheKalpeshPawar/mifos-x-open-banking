/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

/**
 * Stable tags the three UI suites drive this screen by. Append-only: a renamed tag silently drops
 * whichever assertions referenced it.
 */
internal object SendMoneyTestTags {
    const val SKELETON = "sendMoney:skeleton"
    const val STEP_INDICATOR = "sendMoney:stepIndicator"

    const val DEBTOR_LIST = "sendMoney:debtorList"
    const val CREDITOR_LIST = "sendMoney:creditorList"
    const val NO_SAVED_PAYEES = "sendMoney:noSavedPayees"
    const val MANUAL_ENTRY_BUTTON = "sendMoney:manualEntryButton"
    const val MANUAL_SORT_CODE = "sendMoney:manualSortCode"
    const val MANUAL_ACCOUNT_NUMBER = "sendMoney:manualAccountNumber"
    const val MANUAL_NAME = "sendMoney:manualName"
    const val MANUAL_CONFIRM = "sendMoney:manualConfirm"

    const val AMOUNT_FIELD = "sendMoney:amountField"
    const val AMOUNT_ERROR = "sendMoney:amountError"
    const val REFERENCE_FIELD = "sendMoney:referenceField"
    const val REVIEW_BUTTON = "sendMoney:reviewButton"

    const val REVIEW_SUMMARY = "sendMoney:reviewSummary"
    const val REVIEW_FROM = "sendMoney:reviewFrom"
    const val REVIEW_TO = "sendMoney:reviewTo"
    const val REVIEW_AMOUNT = "sendMoney:reviewAmount"
    const val REVIEW_REFERENCE = "sendMoney:reviewReference"
    const val CONFIRM_BUTTON = "sendMoney:confirmButton"
    const val CANCEL_BUTTON = "sendMoney:cancelButton"
    const val REVIEW_HERO = "sendMoney:reviewHero"
    const val REVIEW_PAYEE_CHIP = "sendMoney:reviewPayeeChip"
    const val REVIEW_SENT_VIA = "sendMoney:reviewSentVia"
    const val REVIEW_TOTAL = "sendMoney:reviewTotal"
    const val REVIEW_AUTH_NOTICE = "sendMoney:reviewAuthNotice"
    const val EDIT_PAYMENT_BUTTON = "sendMoney:editPaymentButton"

    const val SUBMITTING_INDICATOR = "sendMoney:submittingIndicator"
    const val SUBMITTING_AMOUNT = "sendMoney:submittingAmount"
    const val SUBMITTING_LOCK_NOTE = "sendMoney:submittingLockNote"

    const val SUCCESS_STATE = "sendMoney:successState"
    const val SUCCESS_AMOUNT = "sendMoney:successAmount"
    const val SUCCESS_STATUS_CHIP = "sendMoney:successStatusChip"
    const val SUCCESS_PAYMENT_ID = "sendMoney:successPaymentId"
    const val VIEW_PAYMENT_STATUS_BUTTON = "sendMoney:viewPaymentStatusButton"

    const val ERROR_STATE = "sendMoney:errorState"
    const val ERROR_SUPPORT_REFERENCE = "sendMoney:errorSupportReference"
    const val RETRY_BUTTON = "sendMoney:retryButton"
    const val REAUTHORISE_BUTTON = "sendMoney:reauthoriseButton"
    const val VIEW_CONSENTS_BUTTON = "sendMoney:viewConsentsButton"
    const val EDIT_AMOUNT_BUTTON = "sendMoney:editAmountButton"
    const val CHANGE_PAYER_BUTTON = "sendMoney:changePayerButton"

    /** One debtor row, keyed by OBIE `AccountId`. */
    fun debtorRow(accountId: String): String = "sendMoney:debtorRow:$accountId"

    /** One payee row, keyed by OBIE `BeneficiaryId`. */
    fun creditorRow(beneficiaryId: String): String = "sendMoney:creditorRow:$beneficiaryId"
}
