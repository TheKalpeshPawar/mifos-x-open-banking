/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import cmp.navigation.generated.resources.Res
import cmp.navigation.generated.resources.home
import cmp.navigation.utils.toObjectNavigationRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.mifosx.openbanking.core.ui.NavigationItem
import org.mifosx.openbanking.feature.home.HomeDestination
import org.mifosx.openbanking.feature.home.HomeRoute

/** A bottom-navigation tab. */
sealed class AuthenticatedNavBarTabItem(
    override val selectedIcon: ImageVector,
    override val icon: ImageVector,
    override val labelRes: StringResource,
    override val contentDescriptionRes: StringResource,
    override val graphRoute: String,
    override val startDestinationRoute: String,
    override val testTag: String,
) : NavigationItem {

    // ─── Consumer ─────────────────────────────────────────────────────────────
    data object HomeTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.Filled.Home,
        icon = Icons.Filled.Home,
        labelRes = Res.string.home,
        contentDescriptionRes = Res.string.home,
        graphRoute = HomeDestination.toObjectNavigationRoute(),
        startDestinationRoute = HomeRoute.toObjectNavigationRoute(),
        testTag = "HomeTab",
    )
}

/** The consumer tab set. */
val consumerNavBarTabs: ImmutableList<AuthenticatedNavBarTabItem> = persistentListOf(
    AuthenticatedNavBarTabItem.HomeTab,
)
