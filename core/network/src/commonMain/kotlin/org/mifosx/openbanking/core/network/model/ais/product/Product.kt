/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.product

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OBIE `OBProduct2`: the product associated with one account.
 *
 * The terms themselves hang off exactly one of [pca] or [bca], depending on the product type; both are
 * null for account types OBIE publishes no product entry for (GlobalMoney, Savings, CreditCard), which
 * is what drives the product screen's empty state.
 */
@Serializable
data class Product(
    @SerialName("ProductName")
    val productName: String? = null,
    @SerialName("ProductId")
    val productId: String? = null,
    @SerialName("AccountId")
    val accountId: String? = null,
    @SerialName("ProductType")
    val productType: String? = null,
    @SerialName("OtherProductType")
    val otherProductType: OtherProductType? = null,
    @SerialName("PCA")
    val pca: ProductBlock? = null,
    @SerialName("BCA")
    val bca: ProductBlock? = null,
)
