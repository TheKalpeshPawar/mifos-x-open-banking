/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.payments

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.network.api.PaymentsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read access to an account's OBP counterparties (payees / beneficiaries). */
interface PaymentsRepository {
    suspend fun listBeneficiaries(accountId: String): Result<List<Counterparty>>
}

class PaymentsRepositoryImpl(
    private val api: PaymentsApi,
    private val config: ObpConfig,
) : PaymentsRepository {

    override suspend fun listBeneficiaries(accountId: String): Result<List<Counterparty>> =
        api.listCounterparties(config.bankId, accountId).toResult().map { it.counterparties }
}
