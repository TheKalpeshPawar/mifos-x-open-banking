/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithPushTransitions

@Serializable
data object SettingsRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) =
    navigate(SettingsRoute, navOptions)

/**
 * Settings destination. The on-screen navigation targets (change password, about, terms, privacy,
 * licences) are exposed as nullable callbacks. A null callback means the target screen is not yet
 * built — the row renders disabled. Wire each one (non-null) as its destination screen lands.
 */
fun NavGraphBuilder.settingsDestination(
    onBackClick: () -> Unit,
    onNavigateToProfile: (() -> Unit)? = null,
    onNavigateToChangePassword: (() -> Unit)? = null,
    onNavigateToAbout: (() -> Unit)? = null,
    onNavigateToTerms: (() -> Unit)? = null,
    onNavigateToPrivacy: (() -> Unit)? = null,
    onNavigateToLicenses: (() -> Unit)? = null,
) {
    composableWithPushTransitions<SettingsRoute> {
        SettingsScreen(
            onBackClick = onBackClick,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToChangePassword = onNavigateToChangePassword,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToTerms = onNavigateToTerms,
            onNavigateToPrivacy = onNavigateToPrivacy,
            onNavigateToLicenses = onNavigateToLicenses,
        )
    }
}
