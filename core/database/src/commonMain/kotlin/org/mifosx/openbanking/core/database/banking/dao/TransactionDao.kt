/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.database.banking.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import org.mifosx.openbanking.core.database.banking.entity.TransactionEntity

/**
 * DAO for the `transactions` cache table backing the transactions Store5 SourceOfTruth.
 *
 * The Store writer replaces one account's rows on each fetch via [clearAccount] + [upsertAll].
 * [observeByAccount] feeds the recent-transactions list (newest first) and the month-to-date
 * spending aggregate.
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY bookingDateTime DESC")
    fun observeByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun clearAccount(accountId: String)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}
