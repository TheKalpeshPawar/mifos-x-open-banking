/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.pfm

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.PersonalDataField
import org.mifosx.openbanking.core.network.api.PfmApi

/** Read + upsert PFM budgets, stored as OBP personal-data-fields. */
interface PfmRepository {
    suspend fun listBudgets(): Result<List<PersonalDataField>>
    suspend fun upsertBudget(name: String, value: String): Result<PersonalDataField>
}

class PfmRepositoryImpl(
    private val api: PfmApi,
) : PfmRepository {

    override suspend fun listBudgets(): Result<List<PersonalDataField>> =
        api.listFields().toResult().map { it.personalDataFields }

    override suspend fun upsertBudget(name: String, value: String): Result<PersonalDataField> =
        api.upsertField(PersonalDataField(name = name, value = value)).toResult()
}
