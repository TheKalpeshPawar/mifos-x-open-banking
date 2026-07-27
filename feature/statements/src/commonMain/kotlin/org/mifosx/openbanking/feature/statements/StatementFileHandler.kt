/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements

/**
 * Hands a downloaded statement's bytes to the platform for saving or sharing.
 *
 * The feature owns the download (fetching the bytes, showing progress and errors) but not what
 * happens to the file afterwards — that is a platform concern: the Android save-to-Downloads intent,
 * the iOS share sheet, the desktop file picker. Modelling it as an injected interface keeps the view
 * model testable against a fake and defers the platform binding to the app layer.
 *
 * The binding is wired later, in the app layer; until then Koin cannot resolve it at runtime, which
 * is expected for this slice.
 */
interface StatementFileHandler {

    /**
     * Delivers a downloaded statement file to the platform.
     *
     * @param fileName Suggested file name, e.g. `statement-STMT-2026-05.pdf`.
     * @param mimeType MIME type of the payload, e.g. `application/pdf`.
     * @param bytes The rendered file contents.
     */
    suspend fun deliver(fileName: String, mimeType: String, bytes: ByteArray)
}
