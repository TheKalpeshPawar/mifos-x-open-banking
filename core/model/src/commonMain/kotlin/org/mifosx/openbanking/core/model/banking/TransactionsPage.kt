/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking

import kotlinx.serialization.Serializable

/**
 * One page of an account's transaction history, plus the cursor for the next page.
 *
 * @property items The transactions in this page.
 * @property nextLink The OBIE `Links.Next` absolute URL to fetch the next page, or null at the end.
 * @property totalPages OBIE `Meta.TotalPages` when the server reports it, else null.
 */
@Serializable
data class TransactionsPage(
    val items: List<TransactionListItem>,
    val nextLink: String?,
    val totalPages: Int?,
) {
    /** True when a further page can be fetched by following [nextLink]. */
    val hasNextPage: Boolean get() = !nextLink.isNullOrBlank()
}
