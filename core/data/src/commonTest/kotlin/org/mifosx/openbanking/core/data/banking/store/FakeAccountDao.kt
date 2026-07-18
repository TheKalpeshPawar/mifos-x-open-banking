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
import kotlinx.coroutines.flow.asStateFlow
import org.mifosx.openbanking.core.database.banking.dao.AccountDao
import org.mifosx.openbanking.core.database.banking.entity.AccountEntity

/**
 * In-memory [AccountDao] backed by a [MutableStateFlow] so the Store5 SourceOfTruth reader re-emits
 * on every write. [ops] records the writer's `clear` then `upsert` sequence for assertions.
 */
class FakeAccountDao : AccountDao {

    val ops = mutableListOf<String>()

    private val rows = MutableStateFlow<List<AccountEntity>>(emptyList())

    val current: List<AccountEntity> get() = rows.value

    override fun observeAll(): Flow<List<AccountEntity>> = rows.asStateFlow()

    override suspend fun upsertAll(accounts: List<AccountEntity>) {
        ops += "upsert"
        rows.value = (rows.value + accounts).associateBy { it.accountId }.values.toList()
    }

    override suspend fun clear() {
        ops += "clear"
        rows.value = emptyList()
    }
}
