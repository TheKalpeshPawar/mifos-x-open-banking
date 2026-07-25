/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.product

import org.mifosx.openbanking.core.data.banking.ProductRepository
import org.mifosx.openbanking.core.model.banking.ProductTerms
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * Hand-written [ProductRepository] fake.
 *
 * The repository is a one-shot suspend call rather than a stream, so this returns a [NetworkResult]
 * directly — there is no `ScreenDataStream` to construct and no buffered refresh trigger to worry about.
 * [result] can be swapped between calls so a test can fail the first fetch and succeed the retry.
 */
class FakeProductRepository(
    var result: NetworkResult<ProductTerms?, NetworkError> =
        NetworkResult.Success(ProductFixtures.terms()),
) : ProductRepository {

    var callCount: Int = 0
        private set

    var observedAccountId: String? = null
        private set

    override suspend fun getProduct(accountId: String): NetworkResult<ProductTerms?, NetworkError> {
        callCount++
        observedAccountId = accountId
        return result
    }
}
