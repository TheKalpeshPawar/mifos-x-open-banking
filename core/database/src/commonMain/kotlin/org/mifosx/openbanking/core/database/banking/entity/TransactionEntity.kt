/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.database.banking.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room cache row for a booked transaction. Backs the transactions Store5 SourceOfTruth so the
 * recent-transactions list and the month-to-date spending aggregate survive process death and
 * render offline.
 *
 * The primary key is a surrogate auto-generated id rather than the OBIE `TransactionId`: pending
 * entries can arrive with a blank id, and the Store writer replaces a whole account's rows on each
 * fetch (delete-by-account then insert), so a surrogate key sidesteps natural-key collisions.
 */
@Entity(
    tableName = "transactions",
    indices = [Index("accountId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val accountId: String,
    val description: String,
    val bookingDateTime: String,
    val amount: String,
    val currency: String,
    val isCredit: Boolean,
)
