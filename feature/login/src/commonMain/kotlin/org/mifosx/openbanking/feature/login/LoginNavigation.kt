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

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/** Root route of the unauthenticated graph (splash → [AuthGraphRoute] → authenticated). */
@Serializable
data object AuthGraphRoute

@Serializable
data object LoginRoute

fun NavController.navigateToAuthGraph(navOptions: NavOptions? = null) {
    navigate(route = AuthGraphRoute, navOptions = navOptions)
}

/**
 * Unauthenticated graph. Shell wiring for Phase 1 — the real OBP `direct_login` /
 * OIDC flow is implemented in Phase 4 (feature wave A) and will add forgot-password
 * + onboarding destinations here.
 */
fun NavGraphBuilder.authGraph() {
    navigation<AuthGraphRoute>(
        startDestination = LoginRoute,
    ) {
        composableWithStayTransitions<LoginRoute> {
            LoginScreen()
        }
    }
}
