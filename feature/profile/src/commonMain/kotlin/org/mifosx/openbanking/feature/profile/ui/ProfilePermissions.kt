/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile.ui

import org.mifosx.openbanking.core.model.hsbcPermission.OBPermission
import org.mifosx.openbanking.core.model.hsbcPermission.PermissionId

/**
 * The four permissions the profile screen reports on, in the order the design renders them.
 *
 * A deliberate subset, not the full consent. The app requests twenty scopes, and listing all of them
 * turns the card into a compliance dump nobody reads. These four are the ones a user can map onto
 * something the app visibly does — see your accounts, your balances, your transactions, and your
 * own identity — so the card answers "what did I agree to" rather than reciting OBIE vocabulary.
 */
internal val PROFILE_PERMISSION_IDS: List<PermissionId> = listOf(
    PermissionId.ReadAccountsDetail,
    PermissionId.ReadBalances,
    PermissionId.ReadTransactionsDetail,
    PermissionId.ReadParty,
)

/**
 * Builds the four permission rows, marking each granted when it is part of the scope this app
 * requests.
 *
 * Kept free of any Compose reference so the mapping is unit-testable on the JVM without a renderer,
 * and read from [OBPermission.ALL] rather than a second hardcoded list — the requested scope is
 * declared there once, and a permission dropped from it must stop reading as granted here.
 */
internal fun profilePermissions(
    requested: List<OBPermission> = OBPermission.ALL,
): List<ProfilePermissionUi> {
    val grantedIds = requested.map { it.id }.toSet()
    return PROFILE_PERMISSION_IDS.map { id ->
        ProfilePermissionUi(id = id, isGranted = id in grantedIds)
    }
}
