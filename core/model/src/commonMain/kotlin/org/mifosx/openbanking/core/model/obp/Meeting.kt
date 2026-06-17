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

/** OBP meeting scheduled between bank staff and a customer. */
@Serializable
data class Meeting(
    @SerialName("meeting_id") val meetingId: String = "",
    val provider: String = "",
    @SerialName("purpose_message") val purposeMessage: String = "",
    @SerialName("scheduled_date") val scheduledDate: String = "",
)

@Serializable
data class MeetingsResponse(
    val meetings: List<Meeting> = emptyList(),
)

/** Body for scheduling a meeting. */
@Serializable
data class MeetingRequest(
    val provider: String,
    @SerialName("provider_id") val providerId: String,
    val date: String,
    @SerialName("purpose_message") val purposeMessage: String,
    @SerialName("staff_user_id") val staffUserId: String = "",
)
