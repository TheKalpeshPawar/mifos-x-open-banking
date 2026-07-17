/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.model.ais.accountDetails.AccountDetailsResponse
import org.mifosx.openbanking.core.model.ais.accounts.AccountsResponse
import org.mifosx.openbanking.core.model.ais.balances.BalancesResponse
import org.mifosx.openbanking.core.model.ais.beneficiaries.BeneficiariesResponse
import org.mifosx.openbanking.core.model.ais.directDebits.DirectDebitsResponse
import org.mifosx.openbanking.core.model.ais.parties.PartiesResponse
import org.mifosx.openbanking.core.model.ais.party.PartyResponse
import org.mifosx.openbanking.core.model.ais.product.ProductResponse
import org.mifosx.openbanking.core.model.ais.scheduledPayments.ScheduledPaymentsResponse
import org.mifosx.openbanking.core.model.ais.standingOrders.StandingOrdersResponse
import org.mifosx.openbanking.core.model.ais.statementDetails.StatementDetailsResponse
import org.mifosx.openbanking.core.model.ais.statementTransactions.StatementTransactionsResponse
import org.mifosx.openbanking.core.model.ais.statements.StatementsResponse
import org.mifosx.openbanking.core.model.ais.transactions.TransactionsResponse
import org.mifosx.openbanking.core.model.hsbcPermission.request.HSBCCreateConsentRequest
import org.mifosx.openbanking.core.model.hsbcPermission.response.HSBCCreateConsentResponse
import org.mifosx.openbanking.core.network.debug.OAuthDebugLog
import org.mifosx.openbanking.core.network.result.toNetworkResult
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

private const val AIS = "v4.0/aisp"

/**
 * HSBC OBIE AIS endpoints. Paths are relative and resolve against the client's `defaultRequest`
 * base URL. Every call returns a [NetworkResult].
 *
 * The `account-access-consents` calls take the **temporary** client-credentials token explicitly
 * (their own `Bearer` header — it is not the stored PSU token and is not persisted). The account
 * data reads carry no token here: the PSU bearer token is added automatically by the client's `Auth`
 * plugin.
 */
class Aisp(
    private val httpClient: HttpClient,
) {

    suspend fun createConsent(
        consentCreationAccessToken: String,
        request: HSBCCreateConsentRequest,
    ): NetworkResult<HSBCCreateConsentResponse, NetworkError> {
        OAuthDebugLog.log(
            "CONSENT-REQUEST",
            "POST $AIS/account-access-consents\nbearer=$consentCreationAccessToken\n" +
                "body=${Json.encodeToString(HSBCCreateConsentRequest.serializer(), request)}",
        )
        return httpClient.post("$AIS/account-access-consents") {
            bearerAuth(consentCreationAccessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.toNetworkResult()
    }

    suspend fun getConsent(
        consentCreationAccessToken: String,
        consentId: String,
    ): NetworkResult<HSBCCreateConsentResponse, NetworkError> =
        httpClient.get("$AIS/account-access-consents/$consentId") {
            bearerAuth(consentCreationAccessToken)
        }.toNetworkResult()

    suspend fun deleteConsent(
        consentCreationAccessToken: String,
        consentId: String,
    ): NetworkResult<Unit, NetworkError> =
        httpClient.delete("$AIS/account-access-consents/$consentId") {
            bearerAuth(consentCreationAccessToken)
        }.toNetworkResult()

    suspend fun getAccounts(): NetworkResult<AccountsResponse, NetworkError> =
        httpClient.get("$AIS/accounts").toNetworkResult()

    suspend fun getAccountDetails(accountId: String): NetworkResult<AccountDetailsResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId").toNetworkResult()

    suspend fun getBalances(accountId: String): NetworkResult<BalancesResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/balances").toNetworkResult()

    suspend fun getBeneficiaries(accountId: String): NetworkResult<BeneficiariesResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/beneficiaries").toNetworkResult()

    suspend fun getDirectDebits(accountId: String): NetworkResult<DirectDebitsResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/direct-debits").toNetworkResult()

    suspend fun getProduct(accountId: String): NetworkResult<ProductResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/product").toNetworkResult()

    suspend fun getParty(accountId: String): NetworkResult<PartyResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/party").toNetworkResult()

    suspend fun getParties(accountId: String): NetworkResult<PartiesResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/parties").toNetworkResult()

    suspend fun getTransactions(accountId: String): NetworkResult<TransactionsResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/transactions").toNetworkResult()

    suspend fun getStandingOrders(accountId: String): NetworkResult<StandingOrdersResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/standing-orders").toNetworkResult()

    suspend fun getScheduledPayments(accountId: String): NetworkResult<ScheduledPaymentsResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/scheduled-payments").toNetworkResult()

    suspend fun getStatements(accountId: String): NetworkResult<StatementsResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/statements").toNetworkResult()

    suspend fun getStatementDetails(
        accountId: String,
        statementId: String,
    ): NetworkResult<StatementDetailsResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/statements/$statementId").toNetworkResult()

    suspend fun getStatementTransactions(
        accountId: String,
        statementId: String,
    ): NetworkResult<StatementTransactionsResponse, NetworkError> =
        httpClient.get("$AIS/accounts/$accountId/statements/$statementId/transactions").toNetworkResult()
}
