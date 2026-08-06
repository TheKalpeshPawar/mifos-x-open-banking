/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.pisp.domesticPayment.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One charge the ASPSP applies to a payment.
 *
 * @property chargeBearer Who pays it, e.g. `BorneByDebtor`.
 * @property type The OBIE charge type, e.g. `UK.OBIE.CHAPSOut`. Note this need not correspond to the
 *   `LocalInstrument` the Initiation declared — the sandbox returns a CHAPS charge against a Faster
 *   Payments instruction.
 */
@Serializable
data class Charge(
    @SerialName("ChargeBearer")
    val chargeBearer: String? = null,
    @SerialName("Type")
    val type: String? = null,
    @SerialName("Amount")
    val amount: ChargeAmount? = null,
)

@Serializable
data class ChargeAmount(
    @SerialName("Amount")
    val amount: String? = null,
    @SerialName("Currency")
    val currency: String? = null,
)
