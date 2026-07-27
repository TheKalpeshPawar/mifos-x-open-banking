/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.store

import org.mifosx.openbanking.core.data.banking.mapper.toConsentSummary
import org.mifosx.openbanking.core.data.util.toThrowable
import org.mifosx.openbanking.core.model.banking.ConsentSummary
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.ConsentCreationScope
import org.mifosx.openbanking.core.network.api.OAuth
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult
import template.core.base.store.infra.StoreFactory

/**
 * Builds the Store5 store backing the consent screens.
 *
 * Kept apart from [BankingStores] because consent management runs on a different credential: the
 * account-data reads carry the PSU bearer token the client attaches automatically, while this mints
 * a temporary client-credentials token per fetch and passes it explicitly. Sharing one object would
 * blur that boundary, and it is the kind of boundary worth keeping visible.
 *
 * The store is in-memory. Consent status is precisely the thing that must not be served stale — a
 * consent revoked from the bank's own app has to read as revoked on the next load.
 */
object ConsentStores {

    /** One consent, keyed by `ConsentId`. Backs both the list (current consent) and detail screens. */
    fun consentDetailStore(aisp: Aisp, oauth: OAuth): Store<String, ConsentSummary> =
        StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { consentId ->
                fetchConsent(aisp, consentManagementToken(oauth), consentId)
            },
        )

    /**
     * Mints the temporary client-credentials token consent management runs under.
     *
     * Deliberately not cached: it is short-lived, cheap to obtain, and caching it here would mean a
     * token outliving the fetch that needed it.
     */
    private suspend fun consentManagementToken(oauth: OAuth): String =
        when (val result = oauth.clientCredentialsToken(ConsentCreationScope.ACCOUNTS)) {
            is NetworkResult.Success -> result.data.accessToken
            is NetworkResult.Error -> throw result.error.toThrowable()
        }

    private suspend fun fetchConsent(aisp: Aisp, token: String, consentId: String): ConsentSummary =
        when (val result = aisp.getConsent(token, consentId)) {
            is NetworkResult.Success -> result.data.toConsentSummary()
            is NetworkResult.Error -> throw result.error.toThrowable()
        }
}
