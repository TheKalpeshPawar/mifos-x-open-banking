/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.model.banking.ConsentSummary
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.network.model.hsbcPermission.response.HSBCCreateConsentResponse

/**
 * Maps the OBIE consent resource into [ConsentSummary].
 *
 * The same payload backs consent creation and consent read, so this reuses
 * [HSBCCreateConsentResponse] rather than declaring a second identical shape. The status string goes
 * through [ConsentStatus.fromString], which already handles both the long form (`Authorised`) and
 * HSBC's short codes (`AUTH`, `AWAU`).
 *
 * Dates pass through untouched: the screens format them, and a value this app cannot parse is more
 * useful on screen in its raw form than silently blanked.
 */
fun HSBCCreateConsentResponse.toConsentSummary(): ConsentSummary = ConsentSummary(
    consentId = data.consentId,
    status = ConsentStatus.fromString(data.status),
    permissions = data.permissions,
    creationDateTime = data.creationDateTime,
    expirationDateTime = data.expirationDateTime,
    transactionFromDateTime = data.transactionFromDateTime,
    transactionToDateTime = data.transactionToDateTime,
)
