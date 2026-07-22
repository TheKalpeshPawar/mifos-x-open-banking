/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.impl

import org.mifosx.openbanking.core.data.banking.StatementFileRepository
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * Thin one-shot gateway over [Aisp.getStatementFile]. Stateless: the bytes flow straight back to the
 * caller, so there is nothing to hold between calls.
 */
internal class StatementFileRepositoryImpl(
    private val aisp: Aisp,
) : StatementFileRepository {

    override suspend fun downloadStatementFile(
        accountId: String,
        statementId: String,
    ): NetworkResult<ByteArray, NetworkError> =
        aisp.getStatementFile(accountId, statementId)
}
