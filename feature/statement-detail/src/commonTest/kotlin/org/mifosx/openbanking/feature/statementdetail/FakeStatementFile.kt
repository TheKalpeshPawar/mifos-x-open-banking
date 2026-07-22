/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

import kotlinx.coroutines.CompletableDeferred
import org.mifosx.openbanking.core.data.banking.StatementFileRepository
import org.mifosx.openbanking.feature.statements.StatementFileHandler
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * Hand-written [StatementFileRepository] the download tests drive directly. Test source sets do not
 * cross Gradle modules, so this is re-declared locally rather than imported from `feature/statements`.
 *
 * [result] is the programmable outcome of the next fetch; [lastAccountId]/[lastStatementId] record the
 * ids the view model asked for. When [gate] is set the fetch suspends on it, so a test can observe the
 * [org.mifosx.openbanking.feature.statementdetail.ui.DownloadState.Downloading] state mid-flight.
 */
class FakeStatementFileRepository(
    var result: NetworkResult<ByteArray, NetworkError> = NetworkResult.Success(ByteArray(0)),
) : StatementFileRepository {

    var lastAccountId: String? = null
        private set
    var lastStatementId: String? = null
        private set

    var gate: CompletableDeferred<Unit>? = null

    override suspend fun downloadStatementFile(
        accountId: String,
        statementId: String,
    ): NetworkResult<ByteArray, NetworkError> {
        lastAccountId = accountId
        lastStatementId = statementId
        gate?.await()
        return result
    }
}

/**
 * Hand-written [StatementFileHandler] recording the single delivery the view model performs on a
 * successful download, so the test can assert the file name, MIME type and bytes handed to the platform.
 */
class FakeStatementFileHandler : StatementFileHandler {

    var deliveredFileName: String? = null
        private set
    var deliveredMimeType: String? = null
        private set
    var deliveredBytes: ByteArray? = null
        private set
    var deliverCount: Int = 0
        private set

    override suspend fun deliver(fileName: String, mimeType: String, bytes: ByteArray) {
        deliveredFileName = fileName
        deliveredMimeType = mimeType
        deliveredBytes = bytes
        deliverCount++
    }
}
