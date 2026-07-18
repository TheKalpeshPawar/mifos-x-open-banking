/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

@Serializable
data object AccountsDestination

@Serializable
data object AccountsRoute

fun NavController.navigateToAccounts(navOptions: NavOptions? = null) {
    navigate(AccountsDestination, navOptions)
}

fun NavGraphBuilder.accountsGraph(
    onNavigateToAccountDetail: () -> Unit,
    onNavigateToConsentReconfirm: () -> Unit,
) {
    navigation<AccountsDestination>(
        startDestination = AccountsRoute,
    ) {
        composableWithStayTransitions<AccountsRoute> {
            AccountsScreen(
                onNavigateToAccountDetail = onNavigateToAccountDetail,
                onNavigateToConsentReconfirm = onNavigateToConsentReconfirm,
            )
        }
    }
}
