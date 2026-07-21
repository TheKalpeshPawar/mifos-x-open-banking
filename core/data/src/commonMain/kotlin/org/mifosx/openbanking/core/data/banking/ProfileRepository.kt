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
import org.mifosx.openbanking.core.model.banking.PartyProfile
import template.core.base.store.screen.ScreenDataStream

/**
 * Reads the connected party's identity for one account.
 *
 * The returned stream is owned by its caller: a fresh one is built per call, bound to the passed
 * [CoroutineScope], and dies with it. There is deliberately no `refresh()` here — retry goes
 * through the stream the caller already holds.
 */
interface ProfileRepository {

    /**
     * Opens a stream over the party store for [accountId], scoped to [scope].
     *
     * @param accountId OBIE `AccountId`. Fixed for the screen's lifetime, so a plain [String]
     *   rather than a flow.
     * @param scope Typically `viewModelScope`; the stream's auto-refresh observer dies with it.
     */
    fun profileStream(accountId: String, scope: CoroutineScope): ScreenDataStream<PartyProfile>
}
