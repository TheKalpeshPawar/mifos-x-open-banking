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
 * A submitted domestic payment as the bank reports it back.
 *
 * @property creationDateTime When the payment was made. Distinct from [statusUpdateDateTime], which
 *   is when its status last moved — HSBC's sandbox happens to return the two as equal even on a
 *   settled payment, but they are different facts and only this one means "submitted".
 * @property expectedSettlementDateTime When the funds are expected to settle. Absent on some
 *   responses, so nullable rather than defaulted.
 * @property charges What the bank is actually charging for this payment. Non-empty in practice —
 *   the sandbox returned a `UK.OBIE.CHAPSOut` charge of £0.05 on a payment whose Initiation declared
 *   `LocalInstrument: UK.OBIE.FPS` — which is why no fee may be stated before this response exists.
 */
@Serializable
data class Data(
    @SerialName("ConsentId")
    val consentId: String? = null,
    @SerialName("Status")
    val status: String? = null,
    @SerialName("CreationDateTime")
    val creationDateTime: String? = null,
    @SerialName("StatusUpdateDateTime")
    val statusUpdateDateTime: String? = null,
    @SerialName("ExpectedExecutionDateTime")
    val expectedExecutionDateTime: String? = null,
    @SerialName("ExpectedSettlementDateTime")
    val expectedSettlementDateTime: String? = null,
    @SerialName("Charges")
    val charges: List<Charge>? = null,
    @SerialName("ReadRefundAccount")
    val readRefundAccount: String? = null,
    @SerialName("Initiation")
    val initiation: Initiation? = null,
    @SerialName("DomesticPaymentId")
    val domesticPaymentId: String? = null,
)
