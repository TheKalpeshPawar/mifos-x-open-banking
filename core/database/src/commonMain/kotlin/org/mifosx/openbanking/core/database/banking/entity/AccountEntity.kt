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

/**
 * Room cache row for a consented account. Backs the accounts Store5 SourceOfTruth so the
 * account switcher and hero card survive process death and render offline.
 *
 * `accountId` is the OBIE-unique key, so it doubles as the primary key.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val accountId: String,
    val accountHolderName: String,
    val accountSubType: String,
    val currency: String,
    val sortCode: String,
    val accountNumber: String,
    val rawIdentification: String = "",

    /**
     * HSBC's free-text `Description`, and the only signal that an account is a Global Money wallet.
     *
     * Persisted rather than derived because a wallet reports `AccountTypeCode: CACC` — identical to a
     * current account — so dropping this column makes the two indistinguishable once the row has been
     * read back out. That is not hypothetical: it is exactly what happened when the field was added to
     * the model and the network mapper but not here, and the wallet was offered as a payer the bank
     * then refused with `U002`.
     */
    val description: String = "",
)
