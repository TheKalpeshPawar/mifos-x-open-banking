/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.products

import org.mifosx.openbanking.feature.products.ui.ProductScope

/** Stable test tags for the Products screen. */
object ProductsTestTags {
    const val TITLE = "products_title"

    fun bankChip(bankId: String) = "products_bank_${bankId.lowercase()}"

    fun scopeTab(scope: ProductScope) = "products_tab_${scope.name.lowercase()}"

    fun productCard(code: String) = "product_${code.lowercase()}"
}
