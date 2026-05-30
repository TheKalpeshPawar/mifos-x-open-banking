/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.obp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OBP bank product. */
@Serializable
data class Product(
    val code: String = "",
    @SerialName("bank_id") val bankId: String = "",
    @SerialName("parent_product_code") val parentProductCode: String = "",
    val label: String = "",
    val category: String = "",
    val family: String = "",
    @SerialName("super_family") val superFamily: String = "",
    @SerialName("more_info_url") val moreInfoUrl: String = "",
    val description: String = "",
)

@Serializable
data class ProductsResponse(
    val products: List<Product> = emptyList(),
)
