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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OBP bank directory entry. Used to resolve a bank id to its display name. */
@Serializable
data class Bank(
    val id: String = "",
    @SerialName("short_name") val shortName: String = "",
    @SerialName("full_name") val fullName: String = "",
    val logo: String = "",
    val website: String = "",
)
