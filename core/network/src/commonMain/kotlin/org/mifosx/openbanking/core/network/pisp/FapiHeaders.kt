/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.pisp

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal const val HEADER_FAPI_INTERACTION_ID = "x-fapi-interaction-id"
internal const val HEADER_FAPI_FINANCIAL_ID = "x-fapi-financial-id"
internal const val HEADER_IDEMPOTENCY_KEY = "x-idempotency-key"
internal const val HEADER_JWS_SIGNATURE = "x-jws-signature"

/** OBIE caps `x-idempotency-key` at 40 characters. */
internal const val IDEMPOTENCY_KEY_MAX_LENGTH = 40

/**
 * Adds the FAPI headers every PISP call carries.
 *
 * A fresh `x-fapi-interaction-id` is minted per request and echoed back by the bank, which is what
 * makes a failed call traceable in support. [financialId] is the ASPSP's Open Banking organisation
 * id; it is omitted when blank rather than sent empty, because the sandbox accepts its absence and
 * an empty value is rejected outright.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun HttpRequestBuilder.fapiHeaders(financialId: String) {
    header(HEADER_FAPI_INTERACTION_ID, Uuid.generateV4().toString())
    if (financialId.isNotBlank()) {
        header(HEADER_FAPI_FINANCIAL_ID, financialId)
    }
}

/**
 * Adds the two headers that only the write calls carry.
 *
 * [idempotencyKey] is supplied by the caller and never generated here: it is staged once per payment
 * and replayed on every retry, so a retry cannot become a second payment instruction. Generating it
 * at call time would defeat that.
 */
internal fun HttpRequestBuilder.writeHeaders(idempotencyKey: String, jwsSignature: String) {
    require(idempotencyKey.isNotBlank() && idempotencyKey.length <= IDEMPOTENCY_KEY_MAX_LENGTH) {
        "x-idempotency-key must be 1..$IDEMPOTENCY_KEY_MAX_LENGTH characters, was ${idempotencyKey.length}"
    }
    header(HEADER_IDEMPOTENCY_KEY, idempotencyKey)
    header(HEADER_JWS_SIGNATURE, jwsSignature)
}
