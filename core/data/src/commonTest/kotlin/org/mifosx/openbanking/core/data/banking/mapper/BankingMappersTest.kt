/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.network.model.ais.accounts.Account
import org.mifosx.openbanking.core.network.model.ais.accounts.AccountsResponse
import org.mifosx.openbanking.core.network.model.ais.balances.Balance
import org.mifosx.openbanking.core.network.model.ais.balances.BalancesResponse
import org.mifosx.openbanking.core.network.model.ais.transactions.CreditorAccount
import org.mifosx.openbanking.core.network.model.ais.transactions.DebtorAccount
import org.mifosx.openbanking.core.network.model.ais.transactions.MerchantDetails
import org.mifosx.openbanking.core.network.model.ais.transactions.Transaction
import org.mifosx.openbanking.core.network.model.ais.transactions.TransactionsResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mifosx.openbanking.core.network.model.ais.accounts.Data as AccountsData
import org.mifosx.openbanking.core.network.model.ais.balances.Amount as BalanceAmount
import org.mifosx.openbanking.core.network.model.ais.balances.Data as BalancesData
import org.mifosx.openbanking.core.network.model.ais.transactions.Amount as TransactionAmount
import org.mifosx.openbanking.core.network.model.ais.transactions.Data as TransactionsData

class BankingMappersTest {

    @Test
    fun accountMapperFlattensNestedSortCodeAndAccountNumber() {
        val response = AccountsResponse(
            data = AccountsData(
                account = listOf(
                    Account(
                        accountId = "acc-1",
                        currency = "GBP",
                        name = "Everyday Current",
                        description = "Current account",
                        account = listOf(
                            Account(
                                schemeName = "UK.OBIE.SortCodeAccountNumber",
                                identification = "40051512345678",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val accounts = response.toBankAccounts()

        assertEquals(1, accounts.size)
        val account = accounts.first()
        assertEquals("acc-1", account.accountId)
        assertEquals("", account.accountHolderName)
        assertEquals("40051512345678", account.identification)
        assertEquals(AccountScheme.SortCode, account.scheme)
        assertEquals("GBP", account.currency)
    }

    /**
     * The description is the only thing that identifies a Global Money wallet.
     *
     * HSBC reports its `AccountTypeCode` as `CACC`, exactly as for a current account, so dropping
     * this field here would make the wallet unrecognisable everywhere downstream — which is what
     * let it be offered as a payer until the bank refused the payment.
     */
    @Test
    fun accountMapperCarriesTheDescriptionThroughForAGlobalMoneyWallet() {
        val response = AccountsResponse(
            data = AccountsData(
                account = listOf(
                    Account(
                        accountId = "acc-gm",
                        currency = "GBP",
                        accountTypeCode = "CACC",
                        description = "GLOBAL MONEY ACCOUNT",
                        account = listOf(
                            Account(
                                schemeName = "UK.OBIE.SortCodeAccountNumber",
                                identification = "80119770009652",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val account = response.toBankAccounts().single()

        assertEquals("GLOBAL MONEY ACCOUNT", account.description)
    }

    @Test
    fun accountMapperPreservesRawIdentificationForAnIban() {
        val response = AccountsResponse(
            data = AccountsData(
                account = listOf(
                    Account(
                        accountId = "acc-iban",
                        name = "Global Money",
                        currency = "EUR",
                        account = listOf(
                            Account(schemeName = "UK.OBIE.IBAN", identification = "GB29HBUK40051512345678"),
                        ),
                    ),
                ),
            ),
        )

        val account = response.toBankAccounts().single()

        assertEquals("GB29HBUK40051512345678", account.identification)
        assertEquals(AccountScheme.Iban, account.scheme)
    }

    @Test
    fun accountMapperPreservesRawIdentificationForACardNumber() {
        val response = AccountsResponse(
            data = AccountsData(
                account = listOf(
                    Account(
                        accountId = "acc-card",
                        name = "Credit Card",
                        currency = "GBP",
                        account = listOf(
                            Account(schemeName = "UK.OBIE.PAN", identification = "4111111111117654"),
                        ),
                    ),
                ),
            ),
        )

        val account = response.toBankAccounts().single()

        assertEquals("4111111111117654", account.identification)
        assertEquals(AccountScheme.Pan, account.scheme)
    }

    @Test
    fun accountMapperDropsAccountsWithoutId() {
        val response = AccountsResponse(
            data = AccountsData(account = listOf(Account(accountId = null, name = "No id"))),
        )

        assertTrue(response.toBankAccounts().isEmpty())
    }

    @Test
    fun accountMapperReturnsEmptyWhenDataNull() {
        assertTrue(AccountsResponse(data = null).toBankAccounts().isEmpty())
    }

    @Test
    fun accountMapperUsesTheNestedIdentificationWhenNoSortCodeScheme() {
        val response = AccountsResponse(
            data = AccountsData(
                account = listOf(
                    Account(
                        accountId = "acc-1",
                        name = "Everyday",
                        currency = "GBP",
                        account = listOf(Account(schemeName = "UK.OBIE.IBAN", identification = "GB1234567890")),
                    ),
                ),
            ),
        )

        val account = response.toBankAccounts().single()

        assertEquals("GB1234567890", account.identification)
        assertEquals(AccountScheme.Iban, account.scheme)
    }

    @Test
    fun accountMapperUsesTopLevelIdentificationAndLeavesNicknameBlankWhenOnlyDescriptionIsPresent() {
        val response = AccountsResponse(
            data = AccountsData(
                account = listOf(
                    Account(
                        accountId = "acc-2",
                        name = null,
                        description = "Current account",
                        currency = null,
                        identification = "99887766554433",
                    ),
                ),
            ),
        )

        val account = response.toBankAccounts().single()

        // Description is free text, not an account name — the holder name is blank so the UI renders
        // a type + last-4 label instead.
        assertEquals("", account.accountHolderName)
        assertEquals("Current account", account.description)
        assertEquals("", account.currency)
        assertEquals("99887766554433", account.identification)
        assertEquals(AccountScheme.Other, account.scheme)
    }

    @Test
    fun accountMapperLeavesNicknameBlankWhenNoBankNameIsPresent() {
        val response = AccountsResponse(
            data = AccountsData(account = listOf(Account(accountId = "only-id"))),
        )

        val account = response.toBankAccounts().single()

        assertEquals("", account.accountHolderName)
        assertEquals("", account.accountTypeCode)
        assertEquals("", account.currency)
        assertEquals("", account.identification)
        assertEquals(AccountScheme.Other, account.scheme)
    }

    @Test
    fun balanceMapperPrefersBookedForCurrentAndAvailableForAvailable() {
        val response = BalancesResponse(
            data = BalancesData(
                balance = listOf(
                    Balance(type = "InterimAvailable", amount = BalanceAmount("2847.63", "GBP")),
                    Balance(type = "InterimBooked", amount = BalanceAmount("2900.00", "GBP")),
                ),
            ),
        )

        val balance = response.toAccountBalance("acc-1")

        assertEquals("acc-1", balance.accountId)
        assertEquals("2900.00", balance.currentAmount)
        assertEquals("2847.63", balance.availableAmount)
        assertEquals("GBP", balance.currency)
    }

    @Test
    fun balanceMapperDefaultsToZeroWhenDataNull() {
        val balance = BalancesResponse(data = null).toAccountBalance("acc-1")

        assertEquals("acc-1", balance.accountId)
        assertEquals("", balance.currency)
        assertEquals("0", balance.currentAmount)
        assertEquals("0", balance.availableAmount)
    }

    @Test
    fun balanceMapperFallsBackToFirstRowWhenNoBookedType() {
        val response = BalancesResponse(
            data = BalancesData(
                balance = listOf(Balance(type = "InterimAvailable", amount = BalanceAmount("50.00", "GBP"))),
            ),
        )

        val balance = response.toAccountBalance("acc-1")

        assertEquals("GBP", balance.currency)
        assertEquals("50.00", balance.currentAmount)
        assertEquals("50.00", balance.availableAmount)
    }

    @Test
    fun balanceMapperTakesCurrencyFromAvailableWhenCurrentHasNone() {
        val response = BalancesResponse(
            data = BalancesData(
                balance = listOf(
                    Balance(type = "InterimBooked", amount = BalanceAmount(amount = "100.00", currency = null)),
                    Balance(type = "InterimAvailable", amount = BalanceAmount(amount = "90.00", currency = "GBP")),
                ),
            ),
        )

        val balance = response.toAccountBalance("acc-1")

        assertEquals("GBP", balance.currency)
        assertEquals("100.00", balance.currentAmount)
        assertEquals("90.00", balance.availableAmount)
    }

    @Test
    fun balanceMapperDefaultsAmountsToZeroAndAvailableToCurrent() {
        val response = BalancesResponse(
            data = BalancesData(
                balance = listOf(
                    Balance(type = "InterimBooked", amount = BalanceAmount(amount = null, currency = "GBP")),
                ),
            ),
        )

        val balance = response.toAccountBalance("acc-1")

        assertEquals("GBP", balance.currency)
        assertEquals("0", balance.currentAmount)
        assertEquals("0", balance.availableAmount)
    }

    @Test
    fun transactionMapperPrefersMerchantNameAndReadsCreditFlag() {
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        transactionId = "t1",
                        creditDebitIndicator = "Debit",
                        bookingDateTime = "2026-06-27T10:00:00Z",
                        transactionInformation = "CARD PAYMENT",
                        amount = TransactionAmount("42.17", "GBP"),
                        merchantDetails = MerchantDetails(merchantName = "TESCO STORES"),
                    ),
                    Transaction(
                        transactionId = "t2",
                        creditDebitIndicator = "Credit",
                        bookingDateTime = "2026-06-25T10:00:00Z",
                        transactionInformation = "SALARY ACME LTD",
                        amount = TransactionAmount("2400.00", "GBP"),
                    ),
                ),
            ),
        )

        val items = response.toTransactionItems("acc-1")

        assertEquals(2, items.size)
        assertEquals("TESCO STORES", items[0].description)
        assertEquals(false, items[0].isCredit)
        assertEquals("SALARY ACME LTD", items[1].description)
        assertEquals(true, items[1].isCredit)
    }

    @Test
    fun transactionMapperReturnsEmptyWhenDataNull() {
        assertTrue(TransactionsResponse(data = null).toTransactionItems("acc-1").isEmpty())
    }

    @Test
    fun transactionMapperUsesNarrativeWhenMerchantBlankAndKeepsExplicitAccountId() {
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        accountId = "acc-explicit",
                        transactionId = "t1",
                        creditDebitIndicator = "Debit",
                        bookingDateTime = "2026-06-27T10:00:00Z",
                        transactionInformation = "NARRATIVE",
                        amount = TransactionAmount("5.00", "GBP"),
                        merchantDetails = MerchantDetails(merchantName = "   "),
                    ),
                ),
            ),
        )

        val item = response.toTransactionItems("fallback").single()

        assertEquals("acc-explicit", item.accountId)
        assertEquals("NARRATIVE", item.description)
    }

    @Test
    fun transactionMapperTreatsTheSandboxPlaceholderMerchantAsAbsentAndUsesTheNarrative() {
        // The HSBC sandbox stubs MerchantName with this constant on every booked transaction; without
        // suppression every row would collapse to it instead of the real per-transaction narrative.
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        transactionId = "t1",
                        transactionInformation = "SALARY ACME LTD",
                        merchantDetails = MerchantDetails(merchantName = "HSBC Sample merchant"),
                    ),
                ),
            ),
        )

        assertEquals("SALARY ACME LTD", response.toTransactionItems("acc-1").single().description)
    }

    @Test
    fun transactionMapperFallsBackToTheCounterpartyNameWhenMerchantIsPlaceholderAndNarrativeBlank() {
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        transactionId = "out",
                        merchantDetails = MerchantDetails(merchantName = "HSBC Sample merchant"),
                        creditorAccount = CreditorAccount(name = "JANE DOE"),
                    ),
                    Transaction(
                        transactionId = "in",
                        merchantDetails = MerchantDetails(merchantName = "HSBC Sample merchant"),
                        debtorAccount = DebtorAccount(name = "ACME LTD"),
                    ),
                ),
            ),
        )

        val items = response.toTransactionItems("acc-1")

        assertEquals("JANE DOE", items[0].description)
        assertEquals("ACME LTD", items[1].description)
    }

    @Test
    fun transactionMapperDefaultsEveryFieldWhenAllAbsent() {
        val response = TransactionsResponse(data = TransactionsData(transaction = listOf(Transaction())))

        val item = response.toTransactionItems("fallback-acc").single()

        assertEquals("", item.transactionId)
        assertEquals("fallback-acc", item.accountId)
        assertEquals("", item.description)
        assertEquals("", item.bookingDateTime)
        assertEquals("0", item.amount)
        assertEquals("", item.currency)
        assertFalse(item.isCredit)
    }
}
