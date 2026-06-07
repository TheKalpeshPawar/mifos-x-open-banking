/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
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

fun NavGraphBuilder.homeGraph(
    onTransfer: () -> Unit,
    onAccounts: () -> Unit,
    onStandingOrders: () -> Unit,
    onViewCards: () -> Unit,
    onFindAtm: () -> Unit,
    onBeneficiaries: () -> Unit,
    onInsights: () -> Unit,
    onBusinessInsights: () -> Unit,
    onDeferred: (String) -> Unit,
) {
    navigation<HomeDestination>(
        startDestination = HomeRoute,
    ) {
        composableWithStayTransitions<HomeRoute> {
            HomeScreen(
                onTransfer = onTransfer,
                onAccounts = onAccounts,
                onStandingOrders = onStandingOrders,
                onViewCards = onViewCards,
                onFindAtm = onFindAtm,
                onBeneficiaries = onBeneficiaries,
                onInsights = onInsights,
                onBusinessInsights = onBusinessInsights,
                onDeferred = onDeferred,
            )
        }
    }
}
