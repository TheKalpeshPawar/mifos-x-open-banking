/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package cmp.navigation.authenticatednavbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.ui.graphics.vector.ImageVector
import cmp.navigation.generated.resources.Res
import cmp.navigation.generated.resources.accounts
import cmp.navigation.generated.resources.home
import cmp.navigation.generated.resources.more
import cmp.navigation.generated.resources.pay
import cmp.navigation.placeholder.SendMoneyRoute
import cmp.navigation.utils.toObjectNavigationRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.mifosx.openbanking.core.ui.NavigationItem
import org.mifosx.openbanking.feature.accounts.AccountsDestination
import org.mifosx.openbanking.feature.accounts.AccountsRoute
import org.mifosx.openbanking.feature.home.HomeDestination
import org.mifosx.openbanking.feature.home.HomeRoute
import org.mifosx.openbanking.feature.settings.SettingsRoute

/**
 * Consumer bottom-nav tabs, resolved from `idea-layer/design-system/app-shell.yaml`:
 *  - Home · Accounts · Pay · More
 *
 * Non-home tabs point at Phase 2 placeholder routes; their real feature modules
 * land in Phases 4–6.
 */
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

    data object AccountsTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.Filled.AccountBalance,
        icon = Icons.Filled.AccountBalance,
        labelRes = Res.string.accounts,
        contentDescriptionRes = Res.string.accounts,
        graphRoute = AccountsDestination.toObjectNavigationRoute(),
        startDestinationRoute = AccountsRoute.toObjectNavigationRoute(),
        testTag = "AccountsTab",
    )

    /**
     * Payment initiation. Still a placeholder — it points at [SendMoneyRoute], which
     * `bankingPlaceholderDestinations()` registers inside the authenticated nav host. Kept flat
     * (`graphRoute == startDestinationRoute`) so the bottom bar stays visible on it.
     */
    data object PayTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.Filled.Payments,
        icon = Icons.Filled.Payments,
        labelRes = Res.string.pay,
        contentDescriptionRes = Res.string.pay,
        graphRoute = SendMoneyRoute.toObjectNavigationRoute(),
        startDestinationRoute = SendMoneyRoute.toObjectNavigationRoute(),
        testTag = "PayTab",
    )

    data object MoreTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.Filled.MoreHoriz,
        icon = Icons.Filled.MoreHoriz,
        labelRes = Res.string.more,
        contentDescriptionRes = Res.string.more,
        graphRoute = SettingsRoute.toObjectNavigationRoute(),
        startDestinationRoute = SettingsRoute.toObjectNavigationRoute(),
        testTag = "MoreTab",
    )
}

/** Consumer bottom-nav set (app-shell.yaml `consumer.bottom_nav`). */
val consumerNavBarTabs: ImmutableList<AuthenticatedNavBarTabItem> = persistentListOf(
    AuthenticatedNavBarTabItem.HomeTab,
    AuthenticatedNavBarTabItem.AccountsTab,
    AuthenticatedNavBarTabItem.PayTab,
    AuthenticatedNavBarTabItem.MoreTab,
)
