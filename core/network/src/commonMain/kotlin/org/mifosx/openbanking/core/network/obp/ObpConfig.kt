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

/**
 * Static OBP connection settings. [baseUrl] + [bankId] default to the OBP sandbox;
 * Phase 7 binds `backend.environments` (local vs production) per flavor/build-type
 * and injects [consumerKey] from secure storage (never hardcoded — CREDS-LIFECYCLE).
 */
data class ObpConfig(
    // Trailing slash is required so Ktorfit relative paths resolve under /obp/.
    val baseUrl: String = "https://apisandbox.openbankproject.com/obp/",
    val bankId: String = "ac.bank.uk",
    val consumerKey: String = "",
)
