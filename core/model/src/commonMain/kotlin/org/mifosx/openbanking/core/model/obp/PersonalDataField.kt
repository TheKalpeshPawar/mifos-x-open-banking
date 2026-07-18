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
 * OBP personal data field — the PFM dashboard stores budgets as fields named
 * `pfm_budget_<categoryId>` with a numeric string value.
 */
@Serializable
data class PersonalDataField(
    val name: String = "",
    val value: String = "",
)

@Serializable
data class PersonalDataFieldsResponse(
    @SerialName("personal_data_fields") val personalDataFields: List<PersonalDataField> = emptyList(),
)
