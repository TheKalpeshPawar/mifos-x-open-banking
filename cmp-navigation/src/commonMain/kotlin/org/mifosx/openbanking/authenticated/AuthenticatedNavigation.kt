/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
@file:Suppress("MatchingDeclarationName")

package org.mifosx.openbanking.authenticated

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import org.mifosx.openbanking.authenticatednavbar.AuthenticatedNavbarRoute
import org.mifosx.openbanking.authenticatednavbar.authenticatedNavbarGraph

@Serializable
internal data object AuthenticatedGraphRoute

internal fun NavController.navigateToAuthenticatedGraph(navOptions: NavOptions? = null) {
    navigate(route = AuthenticatedGraphRoute, navOptions = navOptions)
}

internal fun NavGraphBuilder.authenticatedGraph(
    @Suppress("UnusedParameter") navController: NavController,
) {
    navigation<AuthenticatedGraphRoute>(
        startDestination = AuthenticatedNavbarRoute,
    ) {
        authenticatedNavbarGraph()
    }
}
