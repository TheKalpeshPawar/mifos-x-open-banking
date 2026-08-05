/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.util

import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.network.model.ais.error.ObErrorResponse
import template.core.base.network.NetworkError

/**
 * HSBC's code for "this product does not serve this endpoint".
 *
 * Despite the wording of the accompanying message ("This action is not allowed on the account type
 * in the request") it is *not* about OBIE's `AccountTypeCode` — a `CACC` current account can
 * receive it while another `CACC` does not. It tracks HSBC's finer **product** classification; see
 * `HsbcProductCapability`.
 */
const val OBIE_UNSUPPORTED_PRODUCT_CODE = "U000"

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * Whether this error says the account's product cannot serve the endpoint.
 *
 * Both halves of the guard matter. The status alone is too broad — a 400 has other causes — and
 * the code alone is too loose, since a bare substring match would fire on any body that happened
 * to contain the token.
 */
fun NetworkError.isUnsupportedForProduct(): Boolean =
    this is NetworkError.Client.BadRequest && body.carriesUnsupportedProductCode()

/** Unwraps [RemoteException] so callers holding a `ScreenState.Error` can ask the same question. */
fun Throwable.isUnsupportedForProduct(): Boolean =
    (this as? RemoteException)?.networkError?.isUnsupportedForProduct() == true

/**
 * The ASPSP's own explanation, or `null` when the body carried none.
 *
 * Preferred over app-authored copy: the bank knows why it refused, and inventing a parallel
 * vocabulary means maintaining a translation of someone else's error catalogue.
 */
fun NetworkError.obieMessage(): String? = body?.parseObError()
    ?.errors
    ?.firstNotNullOfOrNull { it.message?.takeIf(String::isNotBlank) }

/** As [obieMessage], unwrapping [RemoteException]. */
fun Throwable.obieMessage(): String? = (this as? RemoteException)?.networkError?.obieMessage()

/**
 * The ASPSP's own error code, e.g. `U008` or `U014`.
 *
 * The payment flow needs this rather than the HTTP status: four of its ten failure modes arrive as
 * `400`, and only the code distinguishes "the amount is outside your limits" from "the instruction
 * diverged from the one you approved" — recoveries that are not interchangeable.
 */
fun NetworkError.obieErrorCode(): String? = body?.parseObError()
    ?.errors
    ?.firstNotNullOfOrNull { it.errorCode?.takeIf(String::isNotBlank) }

/** As [obieErrorCode], unwrapping [RemoteException]. */
fun Throwable.obieErrorCode(): String? = (this as? RemoteException)?.networkError?.obieErrorCode()

/**
 * The envelope's top-level `Id` — the bank's own reference for this failure.
 *
 * Surfaced to the PSU rather than only logged: when a payment fails for a reason they cannot act on,
 * this is the one thing that makes the call traceable in support.
 */
fun NetworkError.obieSupportReference(): String? = body?.parseObError()
    ?.id
    ?.takeIf(String::isNotBlank)

/** As [obieSupportReference], unwrapping [RemoteException]. */
fun Throwable.obieSupportReference(): String? =
    (this as? RemoteException)?.networkError?.obieSupportReference()

private fun String?.carriesUnsupportedProductCode(): Boolean {
    if (this.isNullOrBlank()) return false
    val errors = parseObError()?.errors
    return if (errors != null) {
        errors.any { it.errorCode == OBIE_UNSUPPORTED_PRODUCT_CODE }
    } else {
        // Error bodies are not contract-guaranteed — a gateway can return HTML or a truncated
        // payload. Falling back to a substring keeps a malformed body from being read as "supported"
        // and sending the user back into a call that cannot succeed.
        contains(OBIE_UNSUPPORTED_PRODUCT_CODE)
    }
}

private fun String.parseObError(): ObErrorResponse? =
    runCatching { lenientJson.decodeFromString<ObErrorResponse>(this) }.getOrNull()
