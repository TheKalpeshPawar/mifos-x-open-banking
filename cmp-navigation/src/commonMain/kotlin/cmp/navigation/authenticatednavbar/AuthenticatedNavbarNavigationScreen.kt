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
import androidx.compose.ui.platform.LocalUriHandler
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
import cmp.navigation.placeholder.AtmLocatorRoute
import cmp.navigation.placeholder.BeneficiariesRoute
import cmp.navigation.placeholder.ConsentManagerRoute
import cmp.navigation.placeholder.PartyRoute
import cmp.navigation.placeholder.PfmDashboardRoute
import cmp.navigation.placeholder.ProductsRoute
import cmp.navigation.placeholder.ScheduledPaymentsRoute
import cmp.navigation.placeholder.StatementsRoute
import cmp.navigation.placeholder.TransactionDetailRoute
import cmp.navigation.placeholder.bankingPlaceholderDestinations
import cmp.navigation.ui.KptRootScaffold
import cmp.navigation.ui.ScaffoldNavigationData
import cmp.navigation.ui.rememberKptNavController
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.NavigationItem
import org.mifosx.openbanking.feature.accountdetail.AccountDetailChip
import org.mifosx.openbanking.feature.accountdetail.AccountDetailRoute
import org.mifosx.openbanking.feature.accountdetail.accountDetailScreen
import org.mifosx.openbanking.feature.accounts.accountsGraph
import org.mifosx.openbanking.feature.directdebits.DirectDebitsRoute
import org.mifosx.openbanking.feature.directdebits.directDebitsScreen
import org.mifosx.openbanking.feature.home.HomeDestination
import org.mifosx.openbanking.feature.home.homeGraph
import org.mifosx.openbanking.feature.profile.ProfileRoute
import org.mifosx.openbanking.feature.profile.profileScreen
import org.mifosx.openbanking.feature.settings.settingsScreen
import org.mifosx.openbanking.feature.standingorders.StandingOrdersRoute
import org.mifosx.openbanking.feature.standingorders.standingOrdersScreen
import org.mifosx.openbanking.feature.transactions.TransactionsRoute
import org.mifosx.openbanking.feature.transactions.transactionsScreen
import template.core.base.ui.util.RootTransitionProviders
import cmp.navigation.placeholder.TransactionsRoute as TransactionsPlaceholderRoute

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
    val navigationItems = consumerNavBarTabs
    val startDestination: Any = HomeDestination
    val uriHandler = LocalUriHandler.current

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
            homeGraph(
                onNavigateToTransactions = { navController.navigate(TransactionsPlaceholderRoute) },
                onNavigateToAccountDetail = { accountId -> navController.navigate(AccountDetailRoute(accountId)) },
                onNavigateToStatements = { navController.navigate(StatementsRoute) },
                onNavigateToConsents = { navController.navigate(ConsentManagerRoute) },
                onNavigateToTransactionDetail = { navController.navigate(TransactionDetailRoute) },
                onNavigateToSpending = { navController.navigate(PfmDashboardRoute) },
                onConnectBank = { navController.navigate(ConsentManagerRoute) },
            )
            accountsGraph(
                onNavigateToAccountDetail = { accountId -> navController.navigate(AccountDetailRoute(accountId)) },
                onNavigateToConsentReconfirm = { navController.navigate(ConsentManagerRoute) },
            )
            accountDetailScreen(
                onNavigateToChip = { chip, accountId -> navController.navigateFromChip(chip, accountId) },
                onBack = { navController.popBackStack() },
            )
            transactionsScreen(
                onNavigateToTransactionDetail = { _, _ -> navController.navigate(TransactionDetailRoute) },
                onBack = { navController.popBackStack() },
            )
            directDebitsScreen(onBack = { navController.popBackStack() })
            standingOrdersScreen(onBack = { navController.popBackStack() })
            settingsScreen(
                onNavigateToProfile = { accountId -> navController.navigate(ProfileRoute(accountId)) },
                onNavigateToConsents = { navController.navigate(ConsentManagerRoute) },
                onOpenUrl = { url -> uriHandler.openUri(url) },
            )
            profileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToConsents = { navController.navigate(ConsentManagerRoute) },
            )
            bankingPlaceholderDestinations()
        }
    }
}

/**
 * Resolves an Explore chip to its destination, carrying the account id.
 *
 * Transactions, Direct Debits and Standing Orders have real feature modules today; the rest
 * resolve to their placeholder routes and are repointed as each feature ships.
 */
private fun NavHostController.navigateFromChip(chip: AccountDetailChip, accountId: String) {
    when (chip) {
        AccountDetailChip.Transactions -> navigate(TransactionsRoute(accountId))
        AccountDetailChip.Statements -> navigate(StatementsRoute)
        AccountDetailChip.StandingOrders -> navigate(StandingOrdersRoute(accountId))
        AccountDetailChip.DirectDebits -> navigate(DirectDebitsRoute(accountId))
        AccountDetailChip.ScheduledPayments -> navigate(ScheduledPaymentsRoute)
        AccountDetailChip.Beneficiaries -> navigate(BeneficiariesRoute)
        AccountDetailChip.AtmLocator -> navigate(AtmLocatorRoute)
        AccountDetailChip.Product -> navigate(ProductsRoute)
        AccountDetailChip.Party -> navigate(PartyRoute)
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
