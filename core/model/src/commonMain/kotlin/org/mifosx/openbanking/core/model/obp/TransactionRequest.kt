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

/**
 * OBP transaction-request result. Shape verified against the live v4.0.0 SEPA
 * `transaction-requests` response (2026-06-03). `status` is COMPLETED on a fully
 * processed sandbox payment; `transaction_ids` lists the posted transaction(s).
 */
@Serializable
data class TransactionRequest(
    val id: String = "",
    val type: String = "",
    val status: String = "",
    @SerialName("transaction_ids") val transactionIds: List<String> = emptyList(),
    val charge: TransactionCharge = TransactionCharge(),
    @SerialName("start_date") val startDate: String = "",
)

@Serializable
data class TransactionCharge(
    val summary: String = "",
    val value: AmountOfMoney = AmountOfMoney(),
)

/**
 * GET transaction-requests response. Each entry records who was paid (by counterparty id, or
 * IBAN for SEPA) and when ([TransactionRequestSummary.startDate]) — the authoritative server-side
 * source for "recent recipients" (OBP transaction `other_account` does not reliably identify the
 * payee).
 */
@Serializable
data class TransactionRequestsResponse(
    @SerialName("transaction_requests_with_charges")
    val requests: List<TransactionRequestSummary> = emptyList(),
)

@Serializable
data class TransactionRequestSummary(
    val id: String = "",
    val type: String = "",
    val status: String = "",
    val details: TransactionRequestDetails = TransactionRequestDetails(),
    @SerialName("start_date") val startDate: String = "",
)

@Serializable
data class TransactionRequestDetails(
    @SerialName("to_counterparty") val toCounterparty: TransactionRequestToCounterparty? = null,
    @SerialName("to_sepa") val toSepa: TransactionRequestToSepa? = null,
)

@Serializable
data class TransactionRequestToCounterparty(
    @SerialName("counterparty_id") val counterpartyId: String = "",
)

@Serializable
data class TransactionRequestToSepa(
    val iban: String = "",
)

/**
 * Request body for the SEPA transaction-request. OBP requires the from-account
 * currency to match [value.currency] (else OBP-40003). The recipient is given by IBAN.
 */
@Serializable
data class SepaTransactionRequestBody(
    val to: SepaCounterpartyIban,
    val value: AmountOfMoney,
    val description: String = "",
    @SerialName("charge_policy") val chargePolicy: String = "SHARED",
)

@Serializable
data class SepaCounterpartyIban(
    val iban: String,
)

/**
 * Request body for the COUNTERPARTY transaction-request. Pays a registered counterparty by
 * id — works for any beneficiary regardless of routing scheme (the universal send path).
 */
@Serializable
data class CounterpartyTransactionRequestBody(
    val to: CounterpartyTransferTo,
    val value: AmountOfMoney,
    val description: String = "",
    @SerialName("charge_policy") val chargePolicy: String = "SHARED",
)

@Serializable
data class CounterpartyTransferTo(
    @SerialName("counterparty_id") val counterpartyId: String,
)

/** Request body for the OBP IBAN checker (`POST /account/check/scheme/iban`). */
@Serializable
data class IbanCheckRequest(
    val address: String,
)

/** Response from the OBP IBAN checker. */
@Serializable
data class IbanCheckResponse(
    @SerialName("is_valid") val isValid: Boolean = false,
)

/** Response from the OBP funds-available check (`answer` is "yes" or "no"). */
@Serializable
data class FundsAvailableResponse(
    val answer: String = "",
)
