/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.products

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Product
import org.mifosx.openbanking.core.network.api.ProductsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read access to OBP bank products. */
interface ProductsRepository {
    suspend fun listProducts(): Result<List<Product>>
}

class ProductsRepositoryImpl(
    private val api: ProductsApi,
    private val config: ObpConfig,
) : ProductsRepository {

    override suspend fun listProducts(): Result<List<Product>> =
        api.listProducts(config.bankId).toResult().map { it.products }
}
