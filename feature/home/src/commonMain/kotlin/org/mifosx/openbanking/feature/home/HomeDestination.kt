/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

@Serializable
data object HomeDestination

@Serializable
data object HomeRoute

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    navigate(HomeDestination, navOptions)
}

@Suppress("LongParameterList")
fun NavGraphBuilder.homeGraph(
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccountDetail: (accountId: String) -> Unit,
    onNavigateToStatements: (accountId: String) -> Unit,
    onNavigateToConsents: () -> Unit,
    onNavigateToTransactionDetail: () -> Unit,
    onNavigateToSpending: () -> Unit,
    onConnectBank: () -> Unit,
) {
    navigation<HomeDestination>(
        startDestination = HomeRoute,
    ) {
        composableWithStayTransitions<HomeRoute> {
            HomeScreen(
                onNavigateToTransactions = onNavigateToTransactions,
                onNavigateToAccountDetail = onNavigateToAccountDetail,
                onNavigateToStatements = onNavigateToStatements,
                onNavigateToConsents = onNavigateToConsents,
                onNavigateToTransactionDetail = onNavigateToTransactionDetail,
                onNavigateToSpending = onNavigateToSpending,
                onConnectBank = onConnectBank,
            )
        }
    }
}
