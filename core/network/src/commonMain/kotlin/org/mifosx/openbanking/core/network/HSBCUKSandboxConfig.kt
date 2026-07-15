/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network

import org.mifosx.openbanking.core.network.config.HsbcConfig

data class HSBCUKSandboxConfig(
    val bankHost: String,
    val redirectUri: String,
) {
    companion object {
        val UKPersonal = HSBCUKSandboxConfig(
            bankHost = HsbcConfig.BANK_HOST,
            redirectUri = HsbcConfig.REDIRECT_URI,
        )
    }
}

fun getBaseUrl(config: HSBCUKSandboxConfig): String {
    return "https://${config.bankHost}/obie/open-banking/"
}
