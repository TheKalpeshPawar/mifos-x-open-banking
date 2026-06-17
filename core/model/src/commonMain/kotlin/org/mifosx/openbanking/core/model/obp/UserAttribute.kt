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

/**
 * One user personal-data field (OBP v6 `/my/personal-data-fields`). The app stores PFM
 * budgets here under the `pfm_budget_<categoryId>` naming convention. POSTing an existing
 * name APPENDS a new row (no upsert) — readers must take the newest value per name.
 */
@Serializable
data class UserAttribute(
    @SerialName("user_attribute_id") val userAttributeId: String = "",
    val name: String = "",
    val type: String = "STRING",
    val value: String = "",
    @SerialName("insert_date") val insertDate: String = "",
)

/** Wrapper for `GET /my/personal-data-fields` (`{ "user_attributes": [...] }`). */
@Serializable
data class UserAttributesResponse(
    @SerialName("user_attributes") val userAttributes: List<UserAttribute> = emptyList(),
)

/** Request body for `POST /my/personal-data-fields` (UserAttributeJsonV510 — type required). */
@Serializable
data class UserAttributeRequest(
    val name: String,
    val type: String = "STRING",
    val value: String,
)
