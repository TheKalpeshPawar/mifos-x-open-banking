/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking

import kotlinx.serialization.Serializable

/**
 * How a scheduled payment's `ScheduledPaymentDateTime` should be read.
 *
 * OBIE's `ScheduledType` distinguishes the two dates a future-dated instruction can carry:
 * [Execution] is the date the money leaves the account, [Arrival] the date the funds reach the
 * beneficiary. The distinction changes the chip label, icon and accessibility phrasing, so it is
 * resolved once at the mapper rather than re-parsed per consumer.
 *
 * [Unknown] covers a value HSBC sends that this app does not recognise: the payment still renders,
 * with a neutral label, rather than being dropped.
 */
@Serializable
enum class ScheduledPaymentType {
    Execution,
    Arrival,
    Unknown,
}

/**
 * One future-dated scheduled payment, derived from OBIE
 * `GET /accounts/{AccountId}/scheduled-payments`.
 *
 * Named [ScheduledPaymentItem] rather than `ScheduledPayment` to avoid colliding with the network
 * DTO of that name. Amounts stay as the raw OBIE decimal string — formatting is a display concern
 * and belongs in the view model, which applies the currency and date rendering.
 *
 * @property scheduledPaymentId OBIE `ScheduledPaymentId`; the stable key a card renders under. Empty
 *   string when the payload carried none.
 * @property accountId The account the payment is scheduled from.
 * @property payeeName Beneficiary display name from `CreditorAccount.Name`, e.g. `HMRC Self
 *   Assessment`. Empty string when absent.
 * @property amount Raw OBIE `InstructedAmount.Amount` decimal string, e.g. `842.00`.
 * @property currency ISO-4217 code from `InstructedAmount.Currency`, e.g. `GBP`.
 * @property scheduledDateTime ISO-8601 `ScheduledPaymentDateTime`; formatted for display in the view
 *   model. Empty string when the payload carried none.
 * @property scheduledType Whether [scheduledDateTime] is an execution or an arrival date.
 * @property reference Free-text `Reference` narrative, e.g. `HMRC-SA-2526`.
 * @property creditorIdentification Destination account from `CreditorAccount.Identification` — the UK
 *   sort code and account number, e.g. `08-32-00 12001039`.
 */
@Serializable
data class ScheduledPaymentItem(
    val scheduledPaymentId: String,
    val accountId: String,
    val payeeName: String,
    val amount: String,
    val currency: String,
    val scheduledDateTime: String,
    val scheduledType: ScheduledPaymentType,
    val reference: String,
    val creditorIdentification: String,
)
