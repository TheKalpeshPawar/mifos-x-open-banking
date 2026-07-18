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

/**
 * Maps between the [BankAccount]/[TransactionItem] domain models and their Room cache entities.
 * These back the Store5 SourceOfTruth: the reader maps entity → domain, the writer maps domain →
 * entity. Transaction rows use a surrogate auto-generated id, so [toTransactionEntity] leaves it at
 * its default and the writer replaces an account's rows wholesale on each fetch.
 */
fun AccountEntity.toBankAccount(): BankAccount = BankAccount(
    accountId = accountId,
    nickname = nickname,
    accountSubType = accountSubType,
    currency = currency,
    sortCode = sortCode,
    accountNumber = accountNumber,
    rawIdentification = rawIdentification,
)

fun BankAccount.toAccountEntity(): AccountEntity = AccountEntity(
    accountId = accountId,
    nickname = nickname,
    accountSubType = accountSubType,
    currency = currency,
    sortCode = sortCode,
    accountNumber = accountNumber,
    rawIdentification = rawIdentification,
)

fun TransactionEntity.toTransactionItem(): TransactionItem = TransactionItem(
    transactionId = transactionId,
    accountId = accountId,
    description = description,
    bookingDateTime = bookingDateTime,
    amount = amount,
    currency = currency,
    isCredit = isCredit,
)

fun TransactionItem.toTransactionEntity(): TransactionEntity = TransactionEntity(
    transactionId = transactionId,
    accountId = accountId,
    description = description,
    bookingDateTime = bookingDateTime,
    amount = amount,
    currency = currency,
    isCredit = isCredit,
)
