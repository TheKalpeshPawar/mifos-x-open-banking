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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import cmp.navigation.generated.resources.Res
import cmp.navigation.generated.resources.accounts
import cmp.navigation.generated.resources.home
import cmp.navigation.generated.resources.more
import cmp.navigation.generated.resources.transactions
import cmp.navigation.placeholder.MoreRoute
import cmp.navigation.placeholder.TransactionsRoute
import cmp.navigation.utils.toObjectNavigationRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.mifosx.openbanking.core.ui.NavigationItem
import org.mifosx.openbanking.feature.accounts.AccountsDestination
import org.mifosx.openbanking.feature.accounts.AccountsRoute
import org.mifosx.openbanking.feature.home.HomeDestination
import org.mifosx.openbanking.feature.home.HomeRoute

/**
 * Consumer bottom-nav tabs, resolved from `idea-layer/design-system/app-shell.yaml`:
 *  - Home · Accounts · Transactions · More
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

    data object TransactionsTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        labelRes = Res.string.transactions,
        contentDescriptionRes = Res.string.transactions,
        graphRoute = TransactionsRoute.toObjectNavigationRoute(),
        startDestinationRoute = TransactionsRoute.toObjectNavigationRoute(),
        testTag = "TransactionsTab",
    )

    data object MoreTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.Filled.MoreHoriz,
        icon = Icons.Filled.MoreHoriz,
        labelRes = Res.string.more,
        contentDescriptionRes = Res.string.more,
        graphRoute = MoreRoute.toObjectNavigationRoute(),
        startDestinationRoute = MoreRoute.toObjectNavigationRoute(),
        testTag = "MoreTab",
    )
}

/** Consumer bottom-nav set (app-shell.yaml `consumer.bottom_nav`). */
val consumerNavBarTabs: ImmutableList<AuthenticatedNavBarTabItem> = persistentListOf(
    AuthenticatedNavBarTabItem.HomeTab,
    AuthenticatedNavBarTabItem.AccountsTab,
    AuthenticatedNavBarTabItem.TransactionsTab,
    AuthenticatedNavBarTabItem.MoreTab,
)
