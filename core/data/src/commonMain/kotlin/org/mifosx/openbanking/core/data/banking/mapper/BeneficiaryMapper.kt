/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.network.model.ais.beneficiaries.BeneficiariesResponse
import org.mifosx.openbanking.core.network.model.ais.beneficiaries.Beneficiary

private const val SORT_CODE_SCHEME = "sortcodeaccountnumber"
private const val IBAN_SCHEME = "iban"
private const val PAYM_SCHEME = "paym"
private const val PAN_SCHEME = "pan"

/**
 * Maps the OBIE `OBReadBeneficiary5` payload into the saved-payee list.
 *
 * The bank's order is preserved: the list has no status to sort on, so re-ordering would only make
 * the rows shuffle between refreshes for reasons the user cannot see. The endpoint returns the whole
 * set unpaginated, and the screen filters it client-side, so nothing is trimmed here either.
 *
 * Nothing is dropped — a payee missing an optional field degrades to an empty string (or
 * [BeneficiaryScheme.Account]) and still renders. [accountId] is the requested id, used only when
 * the payload omits its own.
 */
fun BeneficiariesResponse.toBeneficiaryItems(accountId: String): List<BeneficiaryItem> =
    data?.beneficiary.orEmpty().map { it.toBeneficiaryItem(accountId) }

private fun Beneficiary.toBeneficiaryItem(requestedAccountId: String): BeneficiaryItem = BeneficiaryItem(
    beneficiaryId = beneficiaryId.orEmpty(),
    accountId = accountId ?: requestedAccountId,
    creditorName = creditorAccount?.name.orEmpty(),
    scheme = creditorAccount?.schemeName.toBeneficiaryScheme(),
    identification = creditorAccount?.identification.orEmpty(),
    reference = reference.orEmpty(),
)

/**
 * Resolves an OBIE `SchemeName` to the domain enum.
 *
 * Matched on the segment after the last `.` so both the namespaced form
 * (`UK.OBIE.SortCodeAccountNumber`) and a bare `IBAN` resolve, case-insensitively. An unrecognised
 * or absent scheme falls through to [BeneficiaryScheme.Account].
 */
private fun String?.toBeneficiaryScheme(): BeneficiaryScheme =
    when (this?.trim()?.substringAfterLast('.')?.lowercase()) {
        SORT_CODE_SCHEME -> BeneficiaryScheme.SortCode
        IBAN_SCHEME -> BeneficiaryScheme.Iban
        PAYM_SCHEME -> BeneficiaryScheme.Paym
        PAN_SCHEME -> BeneficiaryScheme.Card
        else -> BeneficiaryScheme.Account
    }
