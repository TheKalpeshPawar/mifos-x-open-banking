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
import org.mifosx.openbanking.core.model.obp.PersonalDataField
import org.mifosx.openbanking.core.model.obp.PersonalDataFieldsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP personal-data-field endpoints (PFM budgets). */
interface PfmApi {

    @GET("v6.0.0/my/personal-data-fields")
    suspend fun listFields(): NetworkResult<PersonalDataFieldsResponse, NetworkError>

    @POST("v6.0.0/my/personal-data-fields")
    suspend fun upsertField(
        @Body request: PersonalDataField,
    ): NetworkResult<PersonalDataField, NetworkError>
}
