/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package template.core.base.network

/**
 * Standardized error types for remote or network operations.
 *
 * Unlike the previous enum, every variant carries the detail needed to diagnose and surface the
 * failure: the HTTP [statusCode] (null for transport/parse failures), the raw response [body]
 * (which for OBIE/OAuth holds the real error code — callers branch on it when a status is not
 * enough), and the underlying [cause] (for transport/serialization failures). Used with
 * [NetworkResult.Error].
 *
 * The 4xx family is grouped under [Client] so callers can branch coarse (`is Client`) or fine
 * (`is Client.Unauthorized`). 5xx stays flat as [Server] (it almost always means "retry"), and the
 * non-HTTP failures ([Serialization], [Network], [Unknown]) sit at the top level.
 */
sealed interface NetworkError {

    /** The HTTP status code that produced this error, or `null` for transport/parse failures. */
    val statusCode: Int?

    /** The raw response body captured at the point of failure, if any. */
    val body: String?

    /** The underlying throwable for transport/serialization failures, if any. */
    val cause: Throwable?

    /**
     * HTTP 4xx — the request was rejected by the server. A response WAS received, so [body] is
     * available and [cause] is always null.
     */
    sealed interface Client : NetworkError {
        override val cause: Throwable? get() = null

        /** Authentication failed / token expired (HTTP 401). */
        data class Unauthorized(override val body: String? = null) : Client {
            override val statusCode: Int get() = 401
        }

        /** Authenticated but not permitted — e.g. consent revoked (HTTP 403). */
        data class Forbidden(override val body: String? = null) : Client {
            override val statusCode: Int get() = 403
        }

        /** Too many requests in a given window (HTTP 429). */
        data class RateLimited(override val body: String? = null) : Client {
            override val statusCode: Int get() = 429
        }

        /** The request was malformed or missing required parameters (HTTP 400). */
        data class BadRequest(override val body: String? = null) : Client {
            override val statusCode: Int get() = 400
        }

        /** The requested resource could not be found (HTTP 404). */
        data class NotFound(override val body: String? = null) : Client {
            override val statusCode: Int get() = 404
        }

        /** Any other 4xx not called out above. */
        data class Other(
            override val statusCode: Int?,
            override val body: String? = null,
        ) : Client
    }

    /** HTTP 5xx — the server failed to fulfil a valid request. */
    data class Server(
        override val statusCode: Int?,
        override val body: String? = null,
    ) : NetworkError {
        override val cause: Throwable? get() = null
    }

    /** A 2xx response was received but its body could not be decoded into the expected type. */
    data class Serialization(
        override val cause: Throwable,
    ) : NetworkError {
        override val statusCode: Int? get() = null
        override val body: String? get() = null
    }

    /** No response — a transport-level failure (connectivity, DNS, TLS/mTLS handshake, timeout). */
    data class Network(
        override val cause: Throwable,
    ) : NetworkError {
        override val statusCode: Int? get() = null
        override val body: String? get() = null
    }

    /** A non-HTTP surprise — an unexpected throwable that is not clearly transport-level. */
    data class Unknown(
        override val cause: Throwable? = null,
    ) : NetworkError {
        override val statusCode: Int? get() = null
        override val body: String? get() = null
    }
}
