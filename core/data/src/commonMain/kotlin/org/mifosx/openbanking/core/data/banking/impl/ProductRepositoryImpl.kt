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

import org.mifosx.openbanking.core.data.banking.ProductRepository
import org.mifosx.openbanking.core.data.banking.mapper.toProductTerms
import org.mifosx.openbanking.core.model.banking.ProductTerms
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * Thin one-shot gateway over [Aisp.getProduct], mapping the OBIE payload to [ProductTerms]. Stateless:
 * nothing is held between calls, and there is no store to invalidate on logout.
 */
internal class ProductRepositoryImpl(
    private val aisp: Aisp,
) : ProductRepository {

    override suspend fun getProduct(accountId: String): NetworkResult<ProductTerms?, NetworkError> =
        when (val result = aisp.getProduct(accountId)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toProductTerms(accountId))
            is NetworkResult.Error -> result
        }
}
