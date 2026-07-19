/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.statementTransactions

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatementTransactionsSerializationTest {

    private val json = Json { prettyPrint = false }

    private fun postalAddress() = PostalAddress(
        addressType = "Business",
        department = "Ops",
        subDepartment = "Payments",
        streetName = "High Street",
        buildingNumber = "1",
        postCode = "EC1A 1BB",
        townName = "London",
        countrySubDivision = "England",
        country = "GB",
        addressLine = listOf("Line 1", "Line 2"),
        buildingName = "HSBC Tower",
        careOf = "Front Desk",
        districtName = "City",
        floor = "8",
        postBox = "PO 12",
        room = "801",
        townLocationName = "Central",
        unitNumber = "8A",
    )

    private fun sampleTransaction() = Transaction(
        accountId = "acc-1",
        transactionId = "txn-1",
        transactionReference = "ref-1",
        statementReference = listOf("stmt-1"),
        paymentPurposeCode = "CASH",
        creditDebitIndicator = "Credit",
        status = "Booked",
        transactionMutability = "Mutable",
        bookingDateTime = "2026-01-01T10:00:00Z",
        valueDateTime = "2026-01-01T10:00:00Z",
        transactionInformation = "Salary",
        addressLine = "1 High Street",
        amount = Amount(amount = "100.00", currency = "GBP"),
        chargeAmount = Amount(amount = "0.50", currency = "GBP"),
        currencyExchange = CurrencyExchange(
            sourceCurrency = "GBP",
            targetCurrency = "USD",
            unitCurrency = "GBP",
            exchangeRate = 1.25,
            contractIdentification = "ctr-1",
            quotationDate = "2026-01-01T10:00:00Z",
            instructedAmount = Amount(amount = "125.00", currency = "USD"),
        ),
        bankTransactionCode = BankTransactionCode(code = "ReceivedCreditTransfer", subCode = "DomesticCreditTransfer"),
        proprietaryBankTransactionCode = ProprietaryBankTransactionCode(code = "PMT", issuer = "HSBC"),
        balance = Balance(
            creditDebitIndicator = "Credit",
            type = "InterimBooked",
            amount = Amount(amount = "1000.00", currency = "GBP"),
        ),
        merchantDetails = MerchantDetails(merchantName = "Acme", merchantCategoryCode = "5732"),
        creditorAgent = CreditorAgent(
            schemeName = "UK.OBIE.BICFI",
            identification = "HBUKGB4B",
            name = "HSBC",
            postalAddress = postalAddress(),
        ),
        creditorAccount = CreditorAccount(
            schemeName = "UK.OBIE.SortCodeAccountNumber",
            identification = "40000000000001",
            name = "Jane Doe",
            secondaryIdentification = "sec-1",
        ),
        debtorAgent = DebtorAgent(
            schemeName = "UK.OBIE.BICFI",
            identification = "HBUKGB4B",
            name = "HSBC",
            postalAddress = postalAddress(),
            lei = "LEI-1",
        ),
        debtorAccount = DebtorAccount(
            schemeName = "UK.OBIE.SortCodeAccountNumber",
            identification = "40000000000002",
            name = "John Doe",
            secondaryIdentification = "sec-2",
        ),
        cardInstrument = CardInstrument(
            cardSchemeName = "Visa",
            authorisationType = "ConsentAuthorisation",
            name = "Card",
            identification = "1234",
        ),
        extendedProprietaryBankTransactionCodes = ExtendedProprietaryBankTransactionCode(
            code = "PMT",
            issuer = "HSBC",
            description = "Payment",
        ),
        ultimateDebtor = UltimateDebtor(
            name = "Ultimate Debtor",
            identification = "ud-1",
            lei = "LEI-2",
            schemeName = "UK.OBIE.BICFI",
            postalAddress = postalAddress(),
        ),
        ultimateCreditor = UltimateCreditor(
            name = "Ultimate Creditor",
            identification = "uc-1",
            lei = "LEI-3",
            schemeName = "UK.OBIE.BICFI",
            postalAddress = postalAddress(),
        ),
        categoryPurposeCode = "CASH",
    )

    private fun sampleResponse() = StatementTransactionsResponse(
        data = Data(transaction = listOf(sampleTransaction())),
        links = Links(self = "/self"),
        meta = Meta(totalPages = 3),
    )

    @Test
    fun `full statement transactions response round-trips through JSON`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(StatementTransactionsResponse.serializer(), original)
        val decoded = json.decodeFromString(StatementTransactionsResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `transaction fields survive the round-trip`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(StatementTransactionsResponse.serializer(), original)
        val decoded = json.decodeFromString(StatementTransactionsResponse.serializer(), encoded)
        val txn = decoded.data?.transaction?.first()
        assertEquals("txn-1", txn?.transactionId)
        assertEquals("100.00", txn?.amount?.amount)
        assertEquals("GBP", txn?.amount?.currency)
        assertEquals(1.25, txn?.currencyExchange?.exchangeRate)
        assertEquals("London", txn?.creditorAgent?.postalAddress?.townName)
        assertEquals("/self", decoded.links?.self)
        assertEquals(3, decoded.meta?.totalPages)
    }

    @Test
    fun `data class helpers are exercised`() {
        val txn = sampleTransaction()
        assertEquals(txn, txn.copy())
        assertEquals(txn.hashCode(), txn.copy().hashCode())
        assertTrue(txn.toString().contains("txn-1"))
        val balance = txn.balance
        assertEquals(balance, balance?.copy())
        assertTrue(balance.toString().contains("InterimBooked"))
        val defaults = StatementTransactionsResponse()
        assertEquals(StatementTransactionsResponse(), defaults)
    }
}
