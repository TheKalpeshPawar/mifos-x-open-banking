/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:Suppress("MatchingDeclarationName")

package org.mifosx.openbanking.feature.login

import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithPushTransitions
import template.core.base.ui.nav.composableWithStayTransitions

/** Root route of the unauthenticated graph (splash → [AuthGraphRoute] → authenticated). */
@Serializable
data object AuthGraphRoute

@Serializable
data object LoginRoute

@Serializable
data object ForgotPasswordRoute

fun NavController.navigateToAuthGraph(navOptions: NavOptions? = null) {
    navigate(route = AuthGraphRoute, navOptions = navOptions)
}

fun NavController.navigateToForgotPassword(navOptions: NavOptions? = null) {
    navigate(route = ForgotPasswordRoute, navOptions = navOptions)
}

/**
 * Unauthenticated graph: login + forgot-password.
 *
 * Login success is **not** navigated here — `LoginViewModel` marks the session authenticated and
 * `RootNavViewModel` flips the root graph to the authenticated destination reactively. This graph
 * only owns intra-auth navigation (forgot-password) and launching the OBP OIDC browser flow via
 * the platform [androidx.compose.ui.platform.UriHandler].
 */
fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation<AuthGraphRoute>(
        startDestination = LoginRoute,
    ) {
        composableWithStayTransitions<LoginRoute> {
            val uriHandler = LocalUriHandler.current
            LoginScreen(
                onNavigateToForgotPassword = { navController.navigateToForgotPassword() },
                onLaunchOidcAuth = { authUrl -> uriHandler.openUri(authUrl) },
            )
        }
        composableWithPushTransitions<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
