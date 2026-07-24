/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import template.core.base.store.screen.ScreenDataStream

/**
 * Opens the stream of saved payees for one account.
 *
 * Each call builds a stream bound to the caller's [CoroutineScope], which the caller owns for the
 * lifetime of its screen. The account id is a plain value: it arrives as a navigation argument and
 * is fixed for that lifetime.
 *
 * The stream emits `Content` even when the account has no saved payees; callers apply
 * `emptyIfContent` to turn that into an empty screen state. The whole list is emitted once —
 * searching is a client-side filter over it, not a second query.
 */
interface BeneficiariesRepository {

    fun beneficiariesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<BeneficiaryItem>>
}
