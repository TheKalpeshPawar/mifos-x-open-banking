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

/**
 * The consent screen reached from inside the authenticated app to renew or replace a consent.
 *
 * Distinct from [LoginRoute] (which sits behind onboarding in the root auth graph): this one is a
 * standalone destination the authenticated host registers via [loginRenewScreen], so renewal lands
 * straight on the consent screen without the intro, and cancelling pops back rather than exiting.
 */
@Serializable
data object LoginRenewRoute

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

/**
 * Registers the consent screen as a standalone renewal destination.
 *
 * Renders the same [LoginScreen] the onboarding flow uses, so it reuses the whole create-consent +
 * browser-handoff engine unchanged; the only difference is [onBack], which pops back to the caller on
 * cancel instead of exiting the app.
 */
fun NavGraphBuilder.loginRenewScreen(onBack: () -> Unit) {
    composableWithStayTransitions<LoginRenewRoute> {
        LoginScreen(onBack = onBack)
    }
}
