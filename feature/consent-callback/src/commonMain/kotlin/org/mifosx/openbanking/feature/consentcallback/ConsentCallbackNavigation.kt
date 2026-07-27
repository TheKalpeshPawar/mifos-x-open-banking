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

package org.mifosx.openbanking.feature.consentcallback

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Carries HSBC's redirect URL exactly as the OS delivered it.
 *
 * Deliberately just the URL: parsing it, and checking it against the expected `state`/`nonce`, are
 * the data layer's job (`ConsentCallbackRepository.validateCallback`). Navigation never sees those
 * expected values, so it cannot pass the wrong ones.
 */
@Serializable
data class ConsentCallbackRoute(
    val redirectUrl: String,
)

fun NavGraphBuilder.consentCallbackDestination(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    composableWithStayTransitions<ConsentCallbackRoute> { entry ->
        ConsentCallbackScreen(
            redirectUrl = entry.toRoute<ConsentCallbackRoute>().redirectUrl,
            onNavigateToHome = onNavigateToHome,
            onNavigateToLogin = onNavigateToLogin,
        )
    }
}

fun NavController.navigateToConsentCallback(
    route: ConsentCallbackRoute,
    navOptions: NavOptions? = null,
) {
    navigate(route, navOptions)
}
