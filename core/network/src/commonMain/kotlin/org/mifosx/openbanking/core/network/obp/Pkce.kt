/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network.obp

import okio.ByteString.Companion.encodeUtf8
import kotlin.random.Random

/**
 * RFC 7636 PKCE helpers for the OIDC authorization-code flow. The verifier is a high-entropy
 * random string from the unreserved character set; the S256 challenge is its base64url-encoded
 * (no padding) SHA-256 digest, which the OBP-OIDC provider accepts.
 */
object Pkce {

    private const val UNRESERVED =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

    /** A fresh PKCE code-verifier of [length] unreserved chars (RFC 7636 allows 43–128). */
    fun generateVerifier(length: Int = 64): String =
        buildString(length) { repeat(length) { append(UNRESERVED[Random.nextInt(UNRESERVED.length)]) } }

    /** The S256 code-challenge for [verifier]: base64url(SHA-256(verifier)), padding stripped. */
    fun challengeS256(verifier: String): String =
        verifier.encodeUtf8().sha256().base64Url().trimEnd('=')

    /** An opaque random value for the OAuth `state` (CSRF) parameter. */
    fun randomState(length: Int = 32): String =
        buildString(length) { repeat(length) { append(UNRESERVED[Random.nextInt(UNRESERVED.length)]) } }
}
