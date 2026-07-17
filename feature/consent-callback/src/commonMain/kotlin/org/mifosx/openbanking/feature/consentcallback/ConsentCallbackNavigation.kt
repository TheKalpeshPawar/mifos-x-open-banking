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
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

@Serializable
data class ConsentCallbackRoute(
    val code: String? = null,
    val idToken: String? = null,
    val state: String? = null,
    val error: String? = null,
    val errorDescription: String? = null,
    val expectedState: String,
    val expectedNonce: String,
    val consentId: String,
)

fun NavGraphBuilder.consentCallbackDestination(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    composableWithStayTransitions<ConsentCallbackRoute> {
        ConsentCallbackScreen(
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
