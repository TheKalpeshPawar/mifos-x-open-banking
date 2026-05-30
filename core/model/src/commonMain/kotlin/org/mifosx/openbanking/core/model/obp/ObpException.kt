/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.obp

/**
 * Domain-facing failure for OBP calls. Repositories wrap a transport/HTTP failure
 * in this and surface it via [kotlin.Result.failure]; the `reason` is a stable
 * identifier (e.g. UNAUTHORIZED, SERVER) that the UI layer maps to a message.
 */
class ObpException(
    val reason: String,
    message: String = reason,
) : Exception(message)
