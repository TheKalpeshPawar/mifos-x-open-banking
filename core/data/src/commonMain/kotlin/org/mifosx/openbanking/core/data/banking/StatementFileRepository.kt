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
 * Downloads the rendered file (PDF or CSV) for one statement.
 *
 * A one-shot fetch rather than a stream: the bytes are handed straight to the platform's share or
 * save sheet, never cached and never rendered as screen state, so there is no store to observe. The
 * raw [NetworkResult] is returned so the caller can distinguish a genuine failure from an empty file
 * and surface the appropriate message.
 */
interface StatementFileRepository {

    suspend fun downloadStatementFile(
        accountId: String,
        statementId: String,
    ): NetworkResult<ByteArray, NetworkError>
}
