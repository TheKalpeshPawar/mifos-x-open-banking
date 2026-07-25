/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking

import org.mifosx.openbanking.core.model.banking.ProductTerms
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * Reads the product definition — fees, credit-interest tiers, overdraft rates, features — for one
 * account.
 *
 * A one-shot fetch rather than a stream, and the only repository here without a store behind it. The
 * screen declares `cache_strategy: none` deliberately: the bank can revise product terms at any time,
 * and serving a stale copy would misstate the charges a customer is subject to. Every mount re-reads
 * from the network.
 *
 * A `Success(null)` means the account has no product to show — an absent or empty `Data.Product`, or a
 * product with neither a `PCA` nor a `BCA` block. Callers must render that as an empty state, not as a
 * failure; it is the normal answer for GlobalMoney, Savings and CreditCard accounts.
 */
interface ProductRepository {

    suspend fun getProduct(accountId: String): NetworkResult<ProductTerms?, NetworkError>
}
