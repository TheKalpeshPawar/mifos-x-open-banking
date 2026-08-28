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

package cmp.navigation.authenticated

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import cmp.navigation.authenticatednavhost.AuthenticatedNavHostRoute
import cmp.navigation.authenticatednavhost.authenticatedNavHostGraph
import kotlinx.serialization.Serializable

@Serializable
internal data object AuthenticatedGraphRoute

internal fun NavController.navigateToAuthenticatedGraph(navOptions: NavOptions? = null) {
    navigate(route = AuthenticatedGraphRoute, navOptions = navOptions)
}

internal fun NavGraphBuilder.authenticatedGraph(
    @Suppress("UnusedParameter") navController: NavController,
    onLoggedOut: () -> Unit,
) {
    navigation<AuthenticatedGraphRoute>(
        startDestination = AuthenticatedNavHostRoute,
    ) {
        // The host owns a nested NavHost carrying every authenticated destination.
        // onLoggedOut bubbles a sign-out/revoke up to the root navigator, which owns the auth graph.
        authenticatedNavHostGraph(onLoggedOut = onLoggedOut)
    }
}
