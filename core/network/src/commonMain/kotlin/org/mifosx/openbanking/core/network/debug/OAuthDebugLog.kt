/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.debug

import co.touchlab.kermit.Logger.Companion as KermitLogger

/**
 * TEMPORARY, DELIBERATELY VERBOSE tracer for the HSBC OAuth / consent flow. Logs EVERYTHING —
 * including sensitive material (client assertion JWTs, tokens, request/response bodies) — because the
 * sandbox rejection can only be diagnosed from the exact bytes on the wire.
 *
 * DO NOT ship this to production. Emits through Kermit (KMP) under tag `HSBC_OAUTH`.
 *
 * Grab it with: `adb logcat -s HSBC_OAUTH`
 */
object OAuthDebugLog {
    private const val TAG = "HSBC_OAUTH"

    fun log(step: String, detail: String) {
        // Chunk: logcat truncates a single line around ~4000 chars; JWTs + bodies exceed that.
        "[$step] $detail".chunked(3500).forEachIndexed { i, part ->
            KermitLogger.i(tag = TAG, messageString = if (i == 0) part else "(cont) $part")
        }
    }
}
