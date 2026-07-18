/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.pfm

import org.mifosx.openbanking.core.model.obp.Account

/** PFM analysis scope an account belongs to, derived from its OBP product code. */
enum class PfmScope { PERSONAL, BUSINESS }

/**
 * Scope for this account: a `BUSINESS` product code maps to [PfmScope.BUSINESS]; everything
 * else (CURRENT, SAVINGS, WALLET, blank) is [PfmScope.PERSONAL]. Requires the detail-fetched
 * account — the list endpoint returns a null product code.
 */
val Account.pfmScope: PfmScope
    get() = if (typeOrProduct.equals("BUSINESS", ignoreCase = true)) PfmScope.BUSINESS else PfmScope.PERSONAL
