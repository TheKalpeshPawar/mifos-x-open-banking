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
import org.mifosx.openbanking.core.database.banking.entity.PaymentHistoryEntity

/**
 * DAO for the `payment_history` local snapshot table.
 *
 * [observeRecent] is the app's only consumer — the hub screen. Rows are limited to 5, ordered by
 * creation-time descending, so the most recent payment activity sits at the top regardless of
 * whether it succeeded, is still in flight, or failed.
 *
 * [upsert] replaces a row by primary key, so a status refresh is an in-place update rather than a
 * duplicate.
 */
@Dao
interface PaymentHistoryDao {

    @Query("SELECT * FROM payment_history ORDER BY creationDateTime DESC LIMIT 5")
    fun observeRecent(): Flow<List<PaymentHistoryEntity>>

    @Query("SELECT * FROM payment_history WHERE paymentId = :paymentId")
    fun observeById(paymentId: String): Flow<PaymentHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PaymentHistoryEntity)

    @Query("DELETE FROM payment_history")
    suspend fun clear()
}
