/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.authenticatednavbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.vector.ImageVector
import cmp.navigation.generated.resources.Res
import cmp.navigation.generated.resources.accounts
import cmp.navigation.generated.resources.applications
import cmp.navigation.generated.resources.cards
import cmp.navigation.generated.resources.customers
import cmp.navigation.generated.resources.dashboard
import cmp.navigation.generated.resources.home
import cmp.navigation.generated.resources.messages
import cmp.navigation.generated.resources.more
import cmp.navigation.generated.resources.pay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.mifosx.openbanking.core.ui.NavigationItem
import org.mifosx.openbanking.feature.home.HomeDestination
import org.mifosx.openbanking.feature.home.HomeRoute
import org.mifosx.openbanking.feature.settings.SettingsRoute
import org.mifosx.openbanking.placeholder.AccountApplicationsRoute
import org.mifosx.openbanking.placeholder.AccountsRoute
import org.mifosx.openbanking.placeholder.CardsRoute
import org.mifosx.openbanking.placeholder.CustomerMessagesRoute
import org.mifosx.openbanking.placeholder.CustomerSearchRoute
import org.mifosx.openbanking.placeholder.FoDashboardRoute
import org.mifosx.openbanking.placeholder.SendMoneyRoute
import org.mifosx.openbanking.utils.toObjectNavigationRoute

/**
 * Flavor-aware bottom-nav tabs, resolved from `idea-layer/design-system/app-shell.yaml`:
 *  - consumer:     Home · Accounts · Pay · Cards · More
 *  - fieldOfficer: Dashboard · Customers · Applications · Messages · More
 *
 * Non-home tabs point at Phase 2 placeholder routes; their real feature modules
 * land in Phases 4–6. The active set is chosen at runtime from the `userType`
 * flavor (BuildKonfig.IS_FIELDOFFICER) — see [authenticatedNavBarTabs].
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
        graphRoute = AccountsRoute.toObjectNavigationRoute(),
        startDestinationRoute = AccountsRoute.toObjectNavigationRoute(),
        testTag = "AccountsTab",
    )

    data object PayTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.AutoMirrored.Filled.Send,
        icon = Icons.AutoMirrored.Filled.Send,
        labelRes = Res.string.pay,
        contentDescriptionRes = Res.string.pay,
        graphRoute = SendMoneyRoute.toObjectNavigationRoute(),
        startDestinationRoute = SendMoneyRoute.toObjectNavigationRoute(),
        testTag = "PayTab",
    )

    data object CardsTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.Filled.CreditCard,
        icon = Icons.Filled.CreditCard,
        labelRes = Res.string.cards,
        contentDescriptionRes = Res.string.cards,
        graphRoute = CardsRoute.toObjectNavigationRoute(),
        startDestinationRoute = CardsRoute.toObjectNavigationRoute(),
        testTag = "CardsTab",
    )

    // ─── Field officer ──────────────────────────────────────────────────────────
    data object DashboardTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.Filled.Dashboard,
        icon = Icons.Filled.Dashboard,
        labelRes = Res.string.dashboard,
        contentDescriptionRes = Res.string.dashboard,
        graphRoute = FoDashboardRoute.toObjectNavigationRoute(),
        startDestinationRoute = FoDashboardRoute.toObjectNavigationRoute(),
        testTag = "DashboardTab",
    )

    data object CustomersTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.Filled.People,
        icon = Icons.Filled.People,
        labelRes = Res.string.customers,
        contentDescriptionRes = Res.string.customers,
        graphRoute = CustomerSearchRoute.toObjectNavigationRoute(),
        startDestinationRoute = CustomerSearchRoute.toObjectNavigationRoute(),
        testTag = "CustomersTab",
    )

    data object ApplicationsTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.Filled.Description,
        icon = Icons.Filled.Description,
        labelRes = Res.string.applications,
        contentDescriptionRes = Res.string.applications,
        graphRoute = AccountApplicationsRoute.toObjectNavigationRoute(),
        startDestinationRoute = AccountApplicationsRoute.toObjectNavigationRoute(),
        testTag = "ApplicationsTab",
    )

    data object MessagesTab : AuthenticatedNavBarTabItem(
        selectedIcon = Icons.AutoMirrored.Filled.Message,
        icon = Icons.AutoMirrored.Filled.Message,
        labelRes = Res.string.messages,
        contentDescriptionRes = Res.string.messages,
        graphRoute = CustomerMessagesRoute.toObjectNavigationRoute(),
        startDestinationRoute = CustomerMessagesRoute.toObjectNavigationRoute(),
        testTag = "MessagesTab",
    )

    // ─── Shared ───────────────────────────────────────────────────────────────
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
    AuthenticatedNavBarTabItem.CardsTab,
    AuthenticatedNavBarTabItem.MoreTab,
)

/** Field-officer bottom-nav set (app-shell.yaml `fieldOfficer.bottom_nav`). */
val fieldOfficerNavBarTabs: ImmutableList<AuthenticatedNavBarTabItem> = persistentListOf(
    AuthenticatedNavBarTabItem.DashboardTab,
    AuthenticatedNavBarTabItem.CustomersTab,
    AuthenticatedNavBarTabItem.ApplicationsTab,
    AuthenticatedNavBarTabItem.MessagesTab,
    AuthenticatedNavBarTabItem.MoreTab,
)
