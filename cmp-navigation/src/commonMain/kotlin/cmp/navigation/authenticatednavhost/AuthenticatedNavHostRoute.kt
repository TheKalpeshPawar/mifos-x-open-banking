/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package cmp.navigation.authenticatednavhost

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

@Serializable
data object AuthenticatedNavHostRoute

internal fun NavController.navigateToAuthenticatedNavHost(navOptions: NavOptions? = null) {
    navigate(route = AuthenticatedNavHostRoute, navOptions = navOptions)
}

internal fun NavGraphBuilder.authenticatedNavHostGraph(onLoggedOut: () -> Unit) {
    composableWithStayTransitions<AuthenticatedNavHostRoute> {
        AuthenticatedNavHostScreen(onLoggedOut = onLoggedOut)
    }
}
