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

/** OBP consent (connected-app access grant). */
@Serializable
data class Consent(
    @SerialName("consent_id") val consentId: String = "",
    val status: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("valid_until") val validUntil: String = "",
    @SerialName("consumer_id") val consumerId: String = "",
)

@Serializable
data class ConsentsResponse(
    val consents: List<Consent> = emptyList(),
)
