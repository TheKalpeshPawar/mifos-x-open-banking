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

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Product
import org.mifosx.openbanking.core.network.api.ProductsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read access to OBP bank products. */
interface ProductsRepository {
    /** Durable, offline-first stream of the default bank's products (cache-then-network). */
    fun productsStream(scope: CoroutineScope): ScreenDataStream<List<Product>>

    /** The product catalogue of [bankId] (the configured default bank when blank). */
    suspend fun listProducts(bankId: String = ""): Result<List<Product>>
}

class ProductsRepositoryImpl(
    private val api: ProductsApi,
    private val config: ObpConfig,
    private val productsStore: Store<Unit, List<Product>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : ProductsRepository {

    override fun productsStream(scope: CoroutineScope): ScreenDataStream<List<Product>> =
        productsStore.asScreenStream(
            key = Unit,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "products",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun listProducts(bankId: String): Result<List<Product>> =
        api.listProducts(bankId.ifBlank { config.bankId }).toResult().map { it.products }
}
