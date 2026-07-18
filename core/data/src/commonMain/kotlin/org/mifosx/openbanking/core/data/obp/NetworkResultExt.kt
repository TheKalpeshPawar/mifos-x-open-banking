/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.obp

import org.mifosx.openbanking.core.model.obp.ObpException
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** Collapse the network sealed result into a [kotlin.Result], wrapping errors in [ObpException]. */
internal fun <T> NetworkResult<T, NetworkError>.toResult(): Result<T> = when (this) {
    is NetworkResult.Success -> Result.success(data)
    is NetworkResult.Error -> Result.failure(ObpException(reason = error.name))
}
