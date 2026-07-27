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
 * The terms block hanging off an OBIE `OBProduct2`, under either `PCA` or `BCA`.
 *
 * OBIE types these separately (`OBPCAProductDetails1` / `OBBCAProductDetails1`), but the fields this
 * app reads — the monthly maximum charge, the feature list, the credit-interest tier bands and the
 * overdraft tier bands — are shaped identically in both, so one type serves both keys. Add a
 * dedicated BCA type only when a field is needed that the two do not share.
 */
@Serializable
data class ProductBlock(
    @SerialName("ProductDetails")
    val productDetails: ProductDetails? = null,
    @SerialName("CreditInterest")
    val creditInterest: CreditInterest? = null,
    @SerialName("Overdraft")
    val overdraft: Overdraft? = null,
)
