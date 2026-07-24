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
import org.mifosx.openbanking.core.model.callback.ConsentStatus

/**
 * One account-access-consent as the bank currently reports it, from OBIE
 * `GET /account-access-consents/{ConsentId}`.
 *
 * Serves both consent screens: the list renders many of these partitioned by [status], and the
 * detail screen renders one. Dates stay as the raw ISO-8601 strings the bank sent — formatting and
 * the days-until-expiry arithmetic are display concerns the view models own, and keeping the raw
 * value here means a date this app cannot parse still round-trips rather than being lost.
 *
 * The consent id is the only part of this held locally between sessions (in `ConsentSession`);
 * everything else is re-fetched, so a consent revoked from the bank's own app shows as revoked here
 * on the next load rather than going stale.
 *
 * @property consentId OBIE `ConsentId`, e.g. `aac-fb2c4e8a-7d31-4c9e-9f2a-1b3c5d7e9f01`.
 * @property status The bank's current status for this consent.
 * @property permissions The granted OBIE permission codes, e.g. `ReadAccountsDetail`. Their count is
 *   what the list card reports, so it is derived from this rather than stored separately.
 * @property creationDateTime ISO-8601 `CreationDateTime` — when the PSU connected.
 * @property expirationDateTime ISO-8601 `ExpirationDateTime` — drives the expiry countdown.
 * @property transactionFromDateTime ISO-8601 start of the transaction window the consent covers.
 * @property transactionToDateTime ISO-8601 end of that window.
 */
@Serializable
data class ConsentSummary(
    val consentId: String,
    val status: ConsentStatus,
    val permissions: List<String>,
    val creationDateTime: String,
    val expirationDateTime: String,
    val transactionFromDateTime: String,
    val transactionToDateTime: String,
)
