/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The OBIE error envelope ASPSPs return on a 4xx, e.g.
 *
 * ```json
 * {"Code":"400","Id":"842f0682-…","Message":"Bad Request",
 *  "Errors":[{"ErrorCode":"U000",
 *             "Message":"This action is not allowed on the account type in the request"}]}
 * ```
 *
 * The useful text is in `Errors[].Message` — the top-level `Message` is only the status phrase.
 * Every field is nullable: this is parsed opportunistically from an error body, which no ASPSP
 * guarantees to be well-formed JSON.
 */
@Serializable
data class ObErrorResponse(
    @SerialName("Code")
    val code: String? = null,
    @SerialName("Id")
    val id: String? = null,
    @SerialName("Message")
    val message: String? = null,
    @SerialName("Errors")
    val errors: List<ObError>? = null,
)

@Serializable
data class ObError(
    @SerialName("ErrorCode")
    val errorCode: String? = null,
    @SerialName("Message")
    val message: String? = null,
    @SerialName("Path")
    val path: String? = null,
)
