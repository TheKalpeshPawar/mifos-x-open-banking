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

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AccountsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP account endpoints (DirectLogin token applied by the client plugin). */
interface AccountsApi {

    /** All accounts at [bankId] the authenticated user can access. */
    @GET("v3.0.0/banks/{bankId}/accounts")
    suspend fun listAccounts(
        @Path("bankId") bankId: String,
    ): NetworkResult<AccountsResponse, NetworkError>

    /** Cross-bank accounts for the current user — used by the home balance summary. */
    @GET("v4.0.0/my/accounts")
    suspend fun myAccounts(): NetworkResult<AccountsResponse, NetworkError>

    /** Full detail for a single account. */
    @GET("v3.0.0/banks/{bankId}/accounts/{accountId}/account")
    suspend fun accountDetail(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
    ): NetworkResult<Account, NetworkError>
}
