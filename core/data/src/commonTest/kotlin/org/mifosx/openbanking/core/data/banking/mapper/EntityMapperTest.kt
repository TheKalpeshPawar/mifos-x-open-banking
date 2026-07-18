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
            nickname = "Everyday",
            accountSubType = "CurrentAccount",
            currency = "GBP",
            sortCode = "400515",
            accountNumber = "12345678",
        )

        assertEquals(account, account.toAccountEntity().toBankAccount())
    }

    @Test
    fun accountEntityMapsToDomain() {
        val entity = AccountEntity(
            accountId = "a-2",
            nickname = "Savings",
            accountSubType = "Savings",
            currency = "EUR",
            sortCode = "112233",
            accountNumber = "87654321",
        )

        val domain = entity.toBankAccount()

        assertEquals("a-2", domain.accountId)
        assertEquals("Savings", domain.nickname)
        assertEquals("Savings", domain.accountSubType)
        assertEquals("EUR", domain.currency)
        assertEquals("112233", domain.sortCode)
        assertEquals("87654321", domain.accountNumber)
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
}
