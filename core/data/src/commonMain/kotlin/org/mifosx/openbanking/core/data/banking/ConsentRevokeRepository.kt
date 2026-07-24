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

import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * Withdraws one account-access-consent at the bank.
 *
 * A one-shot rather than a stream, and deliberately separate from [ConsentDetailRepository]: the
 * read path is a cached Store5 stream, this is a mutation that happens once and is never replayed —
 * the same split `feature/statements` makes between its list repository and its file repository.
 *
 * The OBIE idempotency rule is applied here rather than in a view model: a `404` means the consent
 * no longer exists at the bank, which is the outcome the caller wanted, so it is reported as
 * [NetworkResult.Success]. Callers therefore run the same local cleanup for both outcomes and never
 * have to know the status code.
 */
interface ConsentRevokeRepository {

    suspend fun revokeConsent(consentId: String): NetworkResult<Unit, NetworkError>
}
