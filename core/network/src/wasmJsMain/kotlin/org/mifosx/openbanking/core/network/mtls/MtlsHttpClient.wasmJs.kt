/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.mtls

import io.ktor.client.HttpClientConfig

/** mTLS is not achievable on the web — a browser cannot present a client certificate. */
actual fun HttpClientConfig<*>.installMtls(identity: MtlsIdentity): Unit =
    throw UnsupportedOperationException(
        "mTLS is not supported on the web target — a browser cannot present a client certificate.",
    )
