/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.obp

import kotlinx.serialization.Serializable

/** OBP account routing entry, e.g. scheme "IBAN" / "AccountNumber" with its address. */
@Serializable
data class AccountRouting(
    val scheme: String = "",
    val address: String = "",
)
