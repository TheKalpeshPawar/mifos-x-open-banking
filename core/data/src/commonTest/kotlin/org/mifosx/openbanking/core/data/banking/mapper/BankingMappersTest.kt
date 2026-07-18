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

import org.mifosx.openbanking.core.network.model.ais.accounts.Account
import org.mifosx.openbanking.core.network.model.ais.accounts.AccountsResponse
import org.mifosx.openbanking.core.network.model.ais.balances.Balance
import org.mifosx.openbanking.core.network.model.ais.balances.BalancesResponse
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
        assertEquals("Everyday Current", account.nickname)
        assertEquals("400515", account.sortCode)
        assertEquals("12345678", account.accountNumber)
        assertEquals("GBP", account.currency)
        assertEquals("40051512345678", account.rawIdentification)
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

        assertEquals("GB29HBUK40051512345678", account.rawIdentification)
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

        assertEquals("4111111111117654", account.rawIdentification)
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
    fun accountMapperUsesFirstSubAccountIdentificationWhenNoSortCodeScheme() {
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

        assertEquals("GB1234", account.sortCode)
        assertEquals("567890", account.accountNumber)
    }

    @Test
    fun accountMapperUsesTopLevelIdentificationAndFallsBackNicknameToDescription() {
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

        assertEquals("Current account", account.nickname)
        assertEquals("Current account", account.accountSubType)
        assertEquals("", account.currency)
        assertEquals("998877", account.sortCode)
        assertEquals("66554433", account.accountNumber)
    }

    @Test
    fun accountMapperDefaultsIdentificationAndNicknameToIdWhenAllAbsent() {
        val response = AccountsResponse(
            data = AccountsData(account = listOf(Account(accountId = "only-id"))),
        )

        val account = response.toBankAccounts().single()

        assertEquals("only-id", account.nickname)
        assertEquals("", account.accountSubType)
        assertEquals("", account.currency)
        assertEquals("", account.sortCode)
        assertEquals("", account.accountNumber)
    }

    @Test
    fun accountMapperSubTypeFallsBackThroughCategoryThenTypeCode() {
        val response = AccountsResponse(
            data = AccountsData(
                account = listOf(
                    Account(accountId = "cat", accountCategory = "Personal"),
                    Account(accountId = "tc", accountTypeCode = "CACC"),
                ),
            ),
        )

        val accounts = response.toBankAccounts()

        assertEquals("Personal", accounts[0].accountSubType)
        assertEquals("CACC", accounts[1].accountSubType)
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
