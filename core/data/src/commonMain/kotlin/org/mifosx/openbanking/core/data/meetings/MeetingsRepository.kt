/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.meetings

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Meeting
import org.mifosx.openbanking.core.model.obp.MeetingRequest
import org.mifosx.openbanking.core.network.api.MeetingsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read + schedule OBP meetings. */
interface MeetingsRepository {
    suspend fun list(): Result<List<Meeting>>
    suspend fun schedule(request: MeetingRequest): Result<Meeting>
}

class MeetingsRepositoryImpl(
    private val api: MeetingsApi,
    private val config: ObpConfig,
) : MeetingsRepository {

    override suspend fun list(): Result<List<Meeting>> =
        api.listMeetings(config.bankId).toResult().map { it.meetings }

    override suspend fun schedule(request: MeetingRequest): Result<Meeting> =
        api.scheduleMeeting(config.bankId, request).toResult()
}
