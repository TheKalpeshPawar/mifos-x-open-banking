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
import androidx.room3.PrimaryKey
import org.mifosx.openbanking.core.common.AccountScheme

/**
 * Room cache row for a consented account. Backs the accounts Store5 SourceOfTruth so the
 * account switcher and hero card survive process death and render offline.
 *
 * `accountId` is the OBIE-unique key, so it doubles as the primary key.
 *
 * `description` is persisted because it is the only signal that a Global Money wallet differs from a
 * current account — both report `AccountTypeCode: CACC`.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val accountId: String,
    val accountTypeCode: String,
    val currency: String,
    val identification: String = "",
    val scheme: AccountScheme = AccountScheme.Other,
    val description: String = "",
    val accountHolderName: String = "",
)
