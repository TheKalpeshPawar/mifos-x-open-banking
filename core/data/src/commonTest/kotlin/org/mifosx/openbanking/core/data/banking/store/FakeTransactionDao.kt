/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.mifosx.openbanking.core.database.banking.dao.TransactionDao
import org.mifosx.openbanking.core.database.banking.entity.TransactionEntity

/**
 * In-memory [TransactionDao] backed by a [MutableStateFlow] so the Store5 SourceOfTruth reader
 * re-emits on every write. [ops] records the writer's `clearAccount` then `upsert` sequence.
 */
class FakeTransactionDao : TransactionDao {

    val ops = mutableListOf<String>()

    private val rows = MutableStateFlow<List<TransactionEntity>>(emptyList())

    val current: List<TransactionEntity> get() = rows.value

    override fun observeByAccount(accountId: String): Flow<List<TransactionEntity>> =
        rows.map { list -> list.filter { it.accountId == accountId } }

    override suspend fun upsertAll(transactions: List<TransactionEntity>) {
        ops += "upsert"
        rows.value = rows.value + transactions
    }

    override suspend fun clearAccount(accountId: String) {
        ops += "clearAccount"
        rows.value = rows.value.filterNot { it.accountId == accountId }
    }

    override suspend fun clear() {
        ops += "clear"
        rows.value = emptyList()
    }
}
