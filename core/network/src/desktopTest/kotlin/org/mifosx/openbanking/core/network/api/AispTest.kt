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
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.network.model.ais.accounts.AccountsResponse
import org.mifosx.openbanking.core.network.model.ais.product.ProductResponse
import org.mifosx.openbanking.core.network.model.ais.scheduledPayments.ScheduledPaymentsResponse
import org.mifosx.openbanking.core.network.model.ais.standingOrders.StandingOrdersResponse
import org.mifosx.openbanking.core.network.model.ais.statementDetails.StatementDetailsResponse
import org.mifosx.openbanking.core.network.model.ais.statementTransactions.StatementTransactionsResponse
import org.mifosx.openbanking.core.network.model.ais.statements.StatementsResponse
import org.mifosx.openbanking.core.network.model.ais.transactions.TransactionsResponse
import org.mifosx.openbanking.core.network.model.hsbcPermission.request.Data
import org.mifosx.openbanking.core.network.model.hsbcPermission.request.HSBCCreateConsentRequest
import org.mifosx.openbanking.core.network.model.hsbcPermission.request.Risk
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AispTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun aisp(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (HttpRequestData) -> Unit = {},
    ): Aisp = Aisp(
        mockClient { request ->
            onRequest(request)
            respond(body, status, jsonHeaders)
        },
    )

    private val consentRequest = HSBCCreateConsentRequest(
        data = Data(
            expirationDateTime = "2026-12-31T00:00:00Z",
            permissions = listOf("ReadAccountsDetail"),
            transactionFromDateTime = "2026-01-01T00:00:00Z",
            transactionToDateTime = "2026-12-31T00:00:00Z",
        ),
        risk = Risk(),
    )

    @Test
    fun `getAccounts issues a GET and decodes the response`() = runTest {
        var request: HttpRequestData? = null
        val result = aisp("{}", onRequest = { request = it }).getAccounts()
        assertIs<NetworkResult.Success<AccountsResponse>>(result)
        assertEquals(HttpMethod.Get, request?.method)
        assertTrue(request?.url?.encodedPath?.contains("v4.0/aisp/accounts") == true)
    }

    @Test
    fun `createConsent posts to account-access-consents with the temporary bearer`() = runTest {
        var request: HttpRequestData? = null
        val result = aisp("bad", HttpStatusCode.BadRequest, onRequest = { request = it })
            .createConsent("temp-token", consentRequest)

        assertIs<NetworkResult.Error<NetworkError>>(result)
        assertIs<NetworkError.Client.BadRequest>(result.error)
        assertEquals(HttpMethod.Post, request?.method)
        assertTrue(request?.url?.encodedPath?.contains("account-access-consents") == true)
        assertEquals("Bearer temp-token", request?.headers?.get(HttpHeaders.Authorization))
    }

    @Test
    fun `getConsent sends the temporary bearer to the consent path`() = runTest {
        var request: HttpRequestData? = null
        aisp("denied", HttpStatusCode.Forbidden, onRequest = { request = it })
            .getConsent("temp-token", "cid-1")
        assertEquals("Bearer temp-token", request?.headers?.get(HttpHeaders.Authorization))
        assertTrue(request?.url?.encodedPath?.contains("account-access-consents/cid-1") == true)
    }

    @Test
    fun `deleteConsent maps 204 to a Unit success`() = runTest {
        val result = aisp("", HttpStatusCode.NoContent).deleteConsent("temp-token", "cid-1")
        assertIs<NetworkResult.Success<Unit>>(result)
    }

    @Test
    fun `getBalances maps 403 to Forbidden`() = runTest {
        val result = aisp("denied", HttpStatusCode.Forbidden).getBalances("acc-1")
        assertIs<NetworkResult.Error<NetworkError>>(result)
        assertIs<NetworkError.Client.Forbidden>(result.error)
    }

    @Test
    fun `getTransactions GETs the transactions path and decodes DebtorAccount name`() = runTest {
        var request: HttpRequestData? = null
        val body = """
            {"Data":{"Transaction":[{"AccountId":"1","TransactionId":"13754",
            "CreditDebitIndicator":"Debit","Status":"BOOK","BookingDateTime":"2026-07-09T11:40:02+00:00",
            "Amount":{"Amount":"13000.00","Currency":"GBP"},
            "DebtorAccount":{"SchemeName":"UK.OBIE.SortCodeAccountNumber","Identification":"80122590953695","Name":"ACCNT SHORT"}}]},
            "Meta":{"TotalPages":1}}
        """.trimIndent()
        val result = aisp(body, onRequest = { request = it }).getTransactions("acc-1")

        assertIs<NetworkResult.Success<TransactionsResponse>>(result)
        assertEquals(HttpMethod.Get, request?.method)
        assertTrue(request?.url?.encodedPath?.endsWith("accounts/acc-1/transactions") == true)
        assertEquals("ACCNT SHORT", result.data.data?.transaction?.first()?.debtorAccount?.name)
    }

    @Test
    fun `getTransactionsPage follows the absolute Next cursor URL verbatim`() = runTest {
        var request: HttpRequestData? = null
        val nextUrl = "https://secure.sandbox.ob.hsbc.co.uk/obie/open-banking/v4.0/aisp/" +
            "accounts/acc-1/transactions?page=1"
        val body = """
            {"Data":{"Transaction":[{"AccountId":"acc-1","TransactionId":"page2-1",
            "CreditDebitIndicator":"Credit","Status":"BOOK","BookingDateTime":"2026-06-20T09:00:00+00:00",
            "Amount":{"Amount":"2400.00","Currency":"GBP"}}]},
            "Links":{"Self":"$nextUrl"},"Meta":{"TotalPages":2}}
        """.trimIndent()
        val result = aisp(body, onRequest = { request = it }).getTransactionsPage(nextUrl)

        assertIs<NetworkResult.Success<TransactionsResponse>>(result)
        assertEquals(HttpMethod.Get, request?.method)
        assertEquals(nextUrl, request?.url.toString())
        assertEquals("page2-1", result.data.data?.transaction?.first()?.transactionId)
    }

    @Test
    fun `getTransactions surfaces Links Next when more pages remain`() = runTest {
        val next = "https://secure.sandbox.ob.hsbc.co.uk/obie/open-banking/v4.0/aisp/" +
            "accounts/acc-1/transactions?page=1"
        val body = """
            {"Data":{"Transaction":[]},"Links":{"Next":"$next"},"Meta":{"TotalPages":3}}
        """.trimIndent()
        val result = aisp(body).getTransactions("acc-1")

        assertIs<NetworkResult.Success<TransactionsResponse>>(result)
        assertEquals(next, result.data.links?.next)
        assertEquals(3, result.data.meta?.totalPages)
    }

    @Test
    fun `getTransactionsPage maps 429 to RateLimited`() = runTest {
        val result = aisp("slow down", HttpStatusCode.TooManyRequests)
            .getTransactionsPage("https://host/x?page=2")
        assertIs<NetworkResult.Error<NetworkError>>(result)
    }

    @Test
    fun `getStandingOrders GETs the standing-orders path`() = runTest {
        var request: HttpRequestData? = null
        val body = """
            {"Data":{"StandingOrder":[{"AccountId":"123456791","StandingOrderStatusCode":"ACTV",
            "NextPaymentAmount":{"Amount":"12.33","Currency":"GBP"},
            "CreditorAccount":{"SchemeName":"UK.OBIE.SortCodeAccountNumber","Identification":"80200110203349","Name":"Mr Nico"}}]},
            "Meta":{"TotalPages":1}}
        """.trimIndent()
        val result = aisp(body, onRequest = { request = it }).getStandingOrders("acc-1")

        assertIs<NetworkResult.Success<StandingOrdersResponse>>(result)
        assertTrue(request?.url?.encodedPath?.endsWith("accounts/acc-1/standing-orders") == true)
        assertEquals("Mr Nico", result.data.data?.standingOrder?.first()?.creditorAccount?.name)
    }

    @Test
    fun `getScheduledPayments GETs the scheduled-payments path`() = runTest {
        var request: HttpRequestData? = null
        val body = """
            {"Data":{"ScheduledPayment":[{"AccountId":"123456791","ScheduledPaymentId":"7",
            "InstructedAmount":{"Amount":"19.17","Currency":"GBP"}}]},"Meta":{"TotalPages":1}}
        """.trimIndent()
        val result = aisp(body, onRequest = { request = it }).getScheduledPayments("acc-1")

        assertIs<NetworkResult.Success<ScheduledPaymentsResponse>>(result)
        assertTrue(request?.url?.encodedPath?.endsWith("accounts/acc-1/scheduled-payments") == true)
    }

    @Test
    fun `getProduct GETs the product path and decodes the PCA terms`() = runTest {
        var request: HttpRequestData? = null
        val body = """
            {"Data":{"Product":[{"AccountId":"acc-1","ProductId":"HSBC-ADVANCE-PCA-001",
            "ProductType":"PCA","ProductName":"HSBC Advance Account",
            "PCA":{"ProductDetails":{"MonthlyMaximumCharge":"0.00","Features":["No monthly fee"]},
            "CreditInterest":{"TierBandSet":[{"TierBandMethod":"Tiered","TierBand":[
            {"TierValueMinimum":"0.01","BandLimit":"1000","AER":"0.15","ApplicationFrequency":"Monthly"}]}]},
            "Overdraft":{"OverdraftTierBandSet":[{"OverdraftTierBand":[
            {"OverdraftType":"Arranged","EAR":"39.9"}]}]}}}]},"Meta":{"TotalPages":1}}
        """.trimIndent()
        val result = aisp(body, onRequest = { request = it }).getProduct("acc-1")

        val success = assertIs<NetworkResult.Success<ProductResponse>>(result)
        val product = success.data.data?.product?.single()
        assertEquals("HSBC Advance Account", product?.productName)
        assertEquals("0.15", product?.pca?.creditInterest?.tierBandSet?.single()?.tierBand?.single()?.aer)
        assertEquals(
            "39.9",
            product?.pca?.overdraft?.overdraftTierBandSet?.single()?.overdraftTierBand?.single()?.ear,
        )
        assertTrue(request?.url?.encodedPath?.endsWith("accounts/acc-1/product") == true)
    }

    @Test
    fun `getStatements decodes the sandbox-only StatementDescription and StatementFee fields`() = runTest {
        var request: HttpRequestData? = null
        val body = """
            {"Data":{"Statement":[{"AccountId":"1","StatementId":"9a6089c8","Type":"AccountOpening",
            "StatementDescription":["desc line 1","desc line 2"],
            "StatementFee":[{"Description":"Statement fee description 1","CreditDebitIndicator":"Credit",
            "Rate":1,"Type":"UK.OBIE.Annual","Amount":{"Amount":"543.00","Currency":"GBP"}}]}]},
            "Meta":{"TotalPages":1}}
        """.trimIndent()
        val result = aisp(body, onRequest = { request = it }).getStatements("acc-1")

        assertIs<NetworkResult.Success<StatementsResponse>>(result)
        assertTrue(request?.url?.encodedPath?.endsWith("accounts/acc-1/statements") == true)
        val statement = result.data.data?.statement?.first()
        assertEquals(2, statement?.statementDescription?.size)
        assertEquals("543.00", statement?.statementFee?.first()?.amount?.amount)
    }

    @Test
    fun `getStatementDetails targets a single statement by id`() = runTest {
        var request: HttpRequestData? = null
        val result = aisp("{}", onRequest = { request = it }).getStatementDetails("acc-1", "960808")
        assertIs<NetworkResult.Success<StatementDetailsResponse>>(result)
        assertTrue(request?.url?.encodedPath?.endsWith("accounts/acc-1/statements/960808") == true)
    }

    @Test
    fun `getStatementTransactions targets the statement transactions sub-path`() = runTest {
        var request: HttpRequestData? = null
        val result = aisp("{}", onRequest = { request = it }).getStatementTransactions("acc-1", "960808")
        assertIs<NetworkResult.Success<StatementTransactionsResponse>>(result)
        assertTrue(request?.url?.encodedPath?.endsWith("accounts/acc-1/statements/960808/transactions") == true)
    }
}
