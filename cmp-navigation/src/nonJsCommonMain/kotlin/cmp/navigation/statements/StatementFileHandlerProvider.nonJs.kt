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

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import org.mifosx.openbanking.feature.statements.StatementFileHandler

/** Android, desktop and iOS deliver statement bytes through FileKit's native save-file dialog. */
internal actual fun platformStatementFileHandler(): StatementFileHandler = FileKitStatementFileHandler()

/**
 * Saves a downloaded statement to a user-chosen location via FileKit.
 *
 * [FileKit.openFileSaver] presents the platform's save dialog seeded with the suggested name and
 * extension and returns the picked destination, or `null` if the user cancels; the bytes are then
 * written to it. The suggested name and extension are split off the incoming [file name][deliver] so
 * the dialog pre-fills a sensible name (e.g. `statement-STMT-2026-05` + `pdf`).
 */
private class FileKitStatementFileHandler : StatementFileHandler {

    override suspend fun deliver(fileName: String, mimeType: String, bytes: ByteArray) {
        val suggestedName = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        val destination = FileKit.openFileSaver(suggestedName, extension)
        destination?.write(bytes)
    }
}
