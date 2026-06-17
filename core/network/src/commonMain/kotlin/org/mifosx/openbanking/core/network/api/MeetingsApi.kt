/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.Meeting
import org.mifosx.openbanking.core.model.obp.MeetingRequest
import org.mifosx.openbanking.core.model.obp.MeetingsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP meeting endpoints (bank staff). */
interface MeetingsApi {

    @GET("v4.0.0/banks/{bankId}/meetings")
    suspend fun listMeetings(
        @Path("bankId") bankId: String,
    ): NetworkResult<MeetingsResponse, NetworkError>

    @POST("v4.0.0/banks/{bankId}/meetings")
    suspend fun scheduleMeeting(
        @Path("bankId") bankId: String,
        @Body request: MeetingRequest,
    ): NetworkResult<Meeting, NetworkError>
}
