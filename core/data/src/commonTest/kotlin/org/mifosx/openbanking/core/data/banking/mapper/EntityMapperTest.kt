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
import org.mifosx.openbanking.core.database.banking.entity.AccountEntity
import org.mifosx.openbanking.core.database.banking.entity.TransactionEntity
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.TransactionItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntityMapperTest {

    @Test
    fun bankAccountRoundTripsThroughEntity() {
        val account = BankAccount(
            accountId = "a-1",
            accountHolderName = "Everyday",
            accountTypeCode = "CACC",
            currency = "GBP",
            identification = "40051512345678",
            scheme = AccountScheme.SortCode,
        )

        assertEquals(account, account.toAccountEntity().toBankAccount())
    }

    @Test
    fun accountEntityMapsToDomain() {
        val entity = AccountEntity(
            accountId = "a-2",
            accountHolderName = "Savings",
            accountTypeCode = "SVGS",
            currency = "EUR",
            identification = "11223387654321",
            scheme = AccountScheme.SortCode,
        )

        val domain = entity.toBankAccount()

        assertEquals("a-2", domain.accountId)
        assertEquals("Savings", domain.accountHolderName)
        assertEquals("SVGS", domain.accountTypeCode)
        assertEquals("EUR", domain.currency)
        assertEquals("11223387654321", domain.identification)
        assertEquals(AccountScheme.SortCode, domain.scheme)
    }

    @Test
    fun transactionItemRoundTripsThroughEntity() {
        val item = TransactionItem(
            transactionId = "t-1",
            accountId = "a-1",
            description = "TESCO",
            bookingDateTime = "2026-06-27T10:00:00Z",
            amount = "42.17",
            currency = "GBP",
            isCredit = false,
        )

        assertEquals(item, item.toTransactionEntity().toTransactionItem())
    }

    @Test
    fun transactionEntityMapsToDomainDroppingSurrogateId() {
        val entity = TransactionEntity(
            id = 7,
            transactionId = "t-2",
            accountId = "a-1",
            description = "SALARY",
            bookingDateTime = "2026-06-25T10:00:00Z",
            amount = "2400.00",
            currency = "GBP",
            isCredit = true,
        )

        val domain = entity.toTransactionItem()

        assertEquals("t-2", domain.transactionId)
        assertEquals("a-1", domain.accountId)
        assertEquals("SALARY", domain.description)
        assertEquals("2026-06-25T10:00:00Z", domain.bookingDateTime)
        assertEquals("2400.00", domain.amount)
        assertEquals("GBP", domain.currency)
        assertTrue(domain.isCredit)
    }

    /**
     * The description must survive the database, because it is the only thing distinguishing a Global
     * Money wallet from a current account — HSBC reports both as `CACC`.
     *
     * `accountsStore` is one of the two stores with a Room source of truth, so the UI reads accounts
     * back out of the database rather than from the network response. When this field was added to
     * the model and the network mapper but not to [AccountEntity], the round trip silently blanked it
     * and the wallet was offered as a payer that the bank then refused with `U002`. A round trip is
     * asserted here rather than one direction, because either half dropping it reproduces that.
     */
    @Test
    fun accountDescriptionSurvivesTheRoundTripThroughTheDatabase() {
        val wallet = BankAccount(
            accountId = "1123456843",
            accountHolderName = "",
            accountTypeCode = "CACC",
            currency = "GBP",
            identification = "80119770009652",
            scheme = AccountScheme.SortCode,
            description = "GLOBAL MONEY ACCOUNT",
        )

        assertEquals(wallet, wallet.toAccountEntity().toBankAccount())
        assertEquals("GLOBAL MONEY ACCOUNT", wallet.toAccountEntity().toBankAccount().description)
    }
}
