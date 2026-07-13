/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network

data class HSBCUKSandboxConfig(
    val bankHost: String,
    val redirectUri: String,
) {
    companion object {
        val UKPersonal = HSBCUKSandboxConfig(
            bankHost = "secure.sandbox.ob.hsbc.co.uk",
            redirectUri = "https://thekalpeshpawar.github.io/obp-callback/callback/",
        )
    }
}

fun getBaseUrl(config: HSBCUKSandboxConfig): String {
    return "https://${config.bankHost}/obie/open-banking/"
}
