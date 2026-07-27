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
import org.mifosx.openbanking.core.model.banking.ConsentSummary
import template.core.base.store.screen.ScreenDataStream

/**
 * Opens the stream for one consent, keyed by its OBIE `ConsentId`.
 *
 * Each call builds a stream bound to the caller's [CoroutineScope], which the caller owns for the
 * lifetime of its screen. The id is a plain value: it arrives as a navigation argument and is fixed
 * for that lifetime.
 */
interface ConsentDetailRepository {

    fun consentStream(
        consentId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<ConsentSummary>
}
