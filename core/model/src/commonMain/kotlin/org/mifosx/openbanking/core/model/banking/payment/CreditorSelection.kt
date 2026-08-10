/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking.payment

import kotlinx.serialization.Serializable
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme

/**
 * Who is being paid — either a saved payee or an account keyed by hand.
 *
 * @property name The creditor's account-holder name, sent as `CreditorAccount.Name`.
 * @property scheme Which identifier scheme [identification] is expressed in.
 * @property identification The destination account in its raw OBIE form — for a UK sort code that
 *   is the unseparated 14 digits (sort code then account number), not the display form.
 * @property beneficiaryId The saved payee this came from; empty when the PSU keyed it manually.
 * @property isOwnAccount Whether the destination is one of the PSU's own authorised accounts. This
 *   is what selects `Risk.PaymentContextCode` between `TransferToSelf` and `TransferToThirdParty`,
 *   and only the caller holding the account list can decide it.
 */
@Serializable
data class CreditorSelection(
    val name: String,
    val scheme: BeneficiaryScheme,
    val identification: String,
    val beneficiaryId: String = "",
    val isOwnAccount: Boolean = false,
)
