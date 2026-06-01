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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import cmp.navigation.generated.resources.Res
import cmp.navigation.generated.resources.not_connected
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.NavigationItem
import org.mifosx.openbanking.feature.accounts.AccountsScreen
import org.mifosx.openbanking.feature.cards.CardsScreen
import org.mifosx.openbanking.feature.home.HomeDestination
import org.mifosx.openbanking.feature.home.homeGraph
import org.mifosx.openbanking.feature.profile.profileDestination
import org.mifosx.openbanking.feature.settings.notificationDestination
import org.mifosx.openbanking.feature.settings.settingsDestination
import org.mifosx.openbanking.placeholder.AccountApplicationsRoute
import org.mifosx.openbanking.placeholder.AccountDetailRoute
import org.mifosx.openbanking.placeholder.AccountsRoute
import org.mifosx.openbanking.placeholder.CardDetailRoute
import org.mifosx.openbanking.placeholder.CardsRoute
import org.mifosx.openbanking.placeholder.FoDashboardRoute
import org.mifosx.openbanking.placeholder.TransactionDetailRoute
import org.mifosx.openbanking.placeholder.bankingPlaceholderDestinations
import org.mifosx.openbanking.ui.KptRootScaffold
import org.mifosx.openbanking.ui.ScaffoldNavigationData
import org.mifosx.openbanking.ui.rememberKptNavController
import org.openmf.kmptemplate.BuildKonfig
import template.core.base.ui.nav.composableWithStayTransitions
import template.core.base.ui.util.RootTransitionProviders

@Composable
internal fun AuthenticatedNavbarNavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberKptNavController(
        name = "AuthenticatedNavbarScreen",
    ),
    viewModel: AuthenticatedNavbarNavigationViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    val message = stringResource(Res.string.not_connected)
    LaunchedEffect(isOffline) {
        if (isOffline) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = Indefinite,
                )
            }
        }
    }

    AuthenticatedNavbarNavigationScreenContent(
        navController = navController,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
internal fun AuthenticatedNavbarNavigationScreenContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scope = rememberCoroutineScope()

    // Flavor-aware tab set, resolved from the userType build flavor (app-shell.yaml).
    val isFieldOfficer = BuildKonfig.IS_FIELDOFFICER
    val navigationItems = if (isFieldOfficer) fieldOfficerNavBarTabs else consumerNavBarTabs
    val startDestination: Any = if (isFieldOfficer) FoDashboardRoute else HomeDestination

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    KptRootScaffold(
        contentWindowInsets = WindowInsets(0.dp),
        navigationData = ScaffoldNavigationData(
            navigationItems = navigationItems,
            selectedNavigationItem = navigationItems.find {
                navBackStackEntry.isCurrentRoute(route = it.graphRoute)
            },
            onNavigationClick = { navigationItem ->
                navController.navigateToTab(navigationItem)
            },
            shouldShowNavigation = navigationItems.any {
                navBackStackEntry.isCurrentRoute(route = it.startDestinationRoute)
            },
        ),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier,
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = RootTransitionProviders.Enter.fadeIn,
            exitTransition = RootTransitionProviders.Exit.fadeOut,
            popEnterTransition = RootTransitionProviders.Enter.fadeIn,
            popExitTransition = RootTransitionProviders.Exit.fadeOut,
        ) {
            // Consumer Home tab — real shell. The gear shortcut opens Settings as the
            // "More" tab (not a plain push) so Settings lives in the More tab's own
            // back stack. Pushing it onto the Home stack made tab save/restore bring
            // Settings back when returning to the Home tab.
            homeGraph(
                onSettingsClick = { navController.navigateToTab(AuthenticatedNavBarTabItem.MoreTab) },
            )
            profileDestination()
            settingsDestination(onBackClick = navController::popBackStack)
            notificationDestination(onBackClick = navController::popBackStack)

            // Accounts tab — real feature module (Phase 5). Detail + request-account targets
            // remain Phase 2 placeholders until their own feature modules land.
            composableWithStayTransitions<AccountsRoute> {
                AccountsScreen(
                    onAccountClick = { navController.navigate(AccountDetailRoute) },
                    onRequestNewAccount = { navController.navigate(AccountApplicationsRoute) },
                )
            }

            // Cards tab — real feature module. Carousel + per-account transactions are live;
            // card controls (freeze/limit/PIN/report/order) surface a "coming soon" snackbar
            // (no consumer OBP endpoint — see CardsViewModel). card-detail + transaction-detail
            // remain Phase 2 placeholders until their feature modules land.
            composableWithStayTransitions<CardsRoute> {
                CardsScreen(
                    onCardClick = { navController.navigate(CardDetailRoute) },
                    onTransactionClick = { navController.navigate(TransactionDetailRoute) },
                    onDeferred = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                )
            }

            // All other banking destinations (Phase 2 placeholders → real in Phases 4–6).
            bankingPlaceholderDestinations()
        }
    }
}

private fun NavHostController.navigateToTab(tab: NavigationItem) {
    navigate(route = tab.startDestinationRoute) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavBackStackEntry?.isCurrentRoute(route: String): Boolean =
    this
        ?.destination
        ?.hierarchy
        ?.any { it.route == route } == true
