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

package org.mifosx.openbanking.feature.onboarding

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

@Serializable
data object UserOnboardingRoute

fun NavGraphBuilder.onboardingDestination(onNavigateToLogin: () -> Unit) {
    composableWithStayTransitions<UserOnboardingRoute> {
        UserOnboardingScreen(onNavigateToLogin = onNavigateToLogin)
    }
}

fun NavController.navigateToUserOnboarding(navOptions: NavOptions? = null) {
    navigate(UserOnboardingRoute, navOptions)
}
