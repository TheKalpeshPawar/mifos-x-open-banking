/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
@file:Suppress("MatchingDeclarationName")

package org.mifosx.openbanking.core.data.util

import template.core.base.network.NetworkError

/**
 * Custom exception class that wraps a [NetworkError] so a category survives inside a [Throwable].
 *
 * When no explicit message is supplied (the error type is not one we recognise), the received HTTP
 * error [body][NetworkError.body] is used, falling back to the underlying cause's message.
 */
class RemoteException(
    val networkError: NetworkError,
    message: String = networkError.body ?: networkError.cause?.message ?: "Network error",
) : Exception(message)

/** Maps each [NetworkError] to a [RemoteException] with a user-facing message. */
fun NetworkError.toThrowable(): Throwable = when (this) {
    // A 400 from an OBIE ASPSP usually explains itself — e.g. U000's "This action is not allowed
    // on the account type in the request". Prefer that over our own copy: the bank knows why it
    // refused, and paraphrasing loses the detail the user (and we) need. The generic line stands in
    // only when the body carried no parseable message.
    is NetworkError.Client.BadRequest -> RemoteException(
        networkError = this,
        message = obieMessage() ?: "Something went wrong with your request. Please try again.",
    )

    is NetworkError.Client.NotFound -> RemoteException(
        networkError = this,
        message = "The information you're looking for couldn't be found.",
    )

    is NetworkError.Client.Unauthorized -> RemoteException(
        networkError = this,
        message = "You need to sign in to access this content.",
    )

    is NetworkError.Client.Forbidden -> RemoteException(
        networkError = this,
        message = "You don't have permission to access this. Your consent may have been revoked.",
    )

    is NetworkError.Client.RateLimited -> RemoteException(
        networkError = this,
        message = "You're doing that too often. Please wait a moment and try again.",
    )

    is NetworkError.Client.Other -> RemoteException(
        networkError = this,
        message = "Something went wrong with your request. Please try again.",
    )

    is NetworkError.Server -> RemoteException(
        networkError = this,
        message = "We're experiencing technical difficulties. Please try again later.",
    )

    is NetworkError.Serialization -> RemoteException(
        networkError = this,
        message = "We received unexpected data. Please try refreshing the app.",
    )

    is NetworkError.Network -> RemoteException(
        networkError = this,
        message = "Please check your connection and try again.",
    )

    is NetworkError.Unknown -> RemoteException(
        networkError = this,
        message = "Something unexpected happened. Please try again.",
    )
}
