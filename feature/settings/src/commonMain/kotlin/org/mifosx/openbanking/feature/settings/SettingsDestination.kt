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

package org.mifosx.openbanking.feature.settings

import androidx.navigation.NavGraphBuilder
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * The settings route. A tab root reached from the bottom bar, and an object rather than a class
 * because it carries no argument — the account it offers Profile for is read from stored
 * preferences, not passed in.
 *
 * Flat, with no nested graph: settings has a single screen, and wrapping one destination in a
 * graph would add a back-stack entry that nothing navigates within.
 */
@Serializable
data object SettingsRoute

/**
 * Registers the settings screen in the host graph.
 *
 * Every outbound move is the host's: [onNavigateToProfile] receives the stored account id,
 * [onNavigateToConsents] takes no argument, and [onOpenUrl] hands off the legal pages. The feature
 * never sees the route table.
 */
fun NavGraphBuilder.settingsScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToConsents: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    composableWithStayTransitions<SettingsRoute> {
        SettingsScreen(
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToConsents = onNavigateToConsents,
            onOpenUrl = onOpenUrl,
        )
    }
}
