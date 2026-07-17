/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
@file:Suppress("MatchingDeclarationName")

package org.mifosx.openbanking.feature.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import org.mifosx.openbanking.feature.login.onboarding.IntroScreen
import template.core.base.ui.nav.composableWithStayTransitions

@Serializable
data object AuthGraphRoute

@Serializable
data object IntroRoute

@Serializable
data object LoginRoute

fun NavController.navigateToAuthGraph(navOptions: NavOptions? = null) {
    navigate(route = AuthGraphRoute, navOptions = navOptions)
}

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation<AuthGraphRoute>(
        startDestination = IntroRoute,
    ) {
        composableWithStayTransitions<IntroRoute> {
            IntroScreen(onContinue = { navController.navigate(LoginRoute) })
        }
        composableWithStayTransitions<LoginRoute> {
            LoginScreen()
        }
    }
}
