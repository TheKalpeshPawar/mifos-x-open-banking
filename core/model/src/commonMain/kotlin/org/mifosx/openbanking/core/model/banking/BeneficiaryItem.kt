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
 * Which identifier scheme a beneficiary's destination account is expressed in.
 *
 * OBIE's `SchemeName` is a namespaced string (`UK.OBIE.SortCodeAccountNumber`, `UK.OBIE.IBAN`, …).
 * Resolving it to an enum at the mapper keeps the string parsing in one place, and lets the row
 * decide the short label and any formatting the scheme implies — an IBAN reads in four-character
 * groups, a sort code already carries its own separators.
 *
 * [Account] is the fallback for a scheme this app does not recognise, including a missing one: the
 * beneficiary still renders under a neutral label rather than being dropped.
 */
@Serializable
enum class BeneficiaryScheme {
    SortCode,
    Iban,
    Paym,
    Card,
    Account,
}

/**
 * One saved payee, derived from OBIE `GET /accounts/{AccountId}/beneficiaries`.
 *
 * Named [BeneficiaryItem] rather than `Beneficiary` to avoid colliding with the network DTO of that
 * name, matching [ScheduledPaymentItem]. Values stay raw — the display label, avatar initials and
 * any grouping are display concerns the view model and row composable apply.
 *
 * @property beneficiaryId OBIE `BeneficiaryId`; the stable key a row renders under and the seed the
 *   avatar colour is derived from. Empty string when the payload carried none.
 * @property accountId The account the payee is saved against.
 * @property creditorName Account-holder name from `CreditorAccount.Name`, e.g. `EDF Energy`. Empty
 *   string when absent. This is the row headline and one of the two searched fields.
 * @property scheme Which identifier scheme [identification] is expressed in.
 * @property identification Destination account from `CreditorAccount.Identification`, e.g.
 *   `40-12-09 65872310` for a sort code or an unspaced IBAN. Not searched.
 * @property reference Free-text payment reference the PSU assigned, e.g. `RENT-FLAT12`. The row's
 *   trailing cell and the second searched field.
 */
@Serializable
data class BeneficiaryItem(
    val beneficiaryId: String,
    val accountId: String,
    val creditorName: String,
    val scheme: BeneficiaryScheme,
    val identification: String,
    val reference: String,
)
