/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package cmp.navigation.statements

import org.mifosx.openbanking.feature.statements.StatementFileHandler

/**
 * App-layer factory for the platform [StatementFileHandler] bound into the Koin graph.
 *
 * Delivery of a downloaded statement is a platform concern: on Android, desktop and iOS the file is
 * saved through a native "save file" dialog, whereas the web targets have no equivalent yet. Because
 * the underlying FileKit save/write API is non-web only, the real implementation is supplied from
 * `nonJsCommonMain` and the web actual is a no-op — so the expect/actual seam is what keeps the
 * binding compiling on every target while still resolving [StatementFileHandler] for the statements
 * view model at runtime.
 */
internal expect fun platformStatementFileHandler(): StatementFileHandler
