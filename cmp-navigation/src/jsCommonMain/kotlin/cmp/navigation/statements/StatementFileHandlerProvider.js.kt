/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
@file:Suppress("MatchingDeclarationName")

package cmp.navigation.statements

import org.mifosx.openbanking.feature.statements.StatementFileHandler

/**
 * Web (JS and WASM-JS) placeholder for statement delivery.
 *
 * FileKit's file-saver/write primitives are non-web only, and browser statement download is not part
 * of this slice, so the web actual is an intentional no-op: it satisfies the Koin binding so the
 * statements view model still resolves on the web targets, but performs no save. Wire a browser
 * download (e.g. FileKit's web `download`) here when statements ship on web.
 */
internal actual fun platformStatementFileHandler(): StatementFileHandler = NoOpStatementFileHandler()

private class NoOpStatementFileHandler : StatementFileHandler {

    override suspend fun deliver(fileName: String, mimeType: String, bytes: ByteArray) {
        // intentional-noop: web statement download not yet supported (see file KDoc).
    }
}
