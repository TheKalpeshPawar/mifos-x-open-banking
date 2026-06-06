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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import cmp.navigation.generated.resources.Res
import cmp.navigation.generated.resources.not_connected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.NavigationItem
import org.mifosx.openbanking.feature.accounts.AccountDetailScreen
import org.mifosx.openbanking.feature.accounts.AccountsScreen
import org.mifosx.openbanking.feature.beneficiaries.BeneficiariesScreen
import org.mifosx.openbanking.feature.cards.CardsScreen
import org.mifosx.openbanking.feature.home.HomeDestination
import org.mifosx.openbanking.feature.home.homeGraph
import org.mifosx.openbanking.feature.profile.navigateToProfile
import org.mifosx.openbanking.feature.profile.profileDestination
import org.mifosx.openbanking.feature.sendmoney.SendMoneyConfirmScreen
import org.mifosx.openbanking.feature.sendmoney.SendMoneyHubScreen
import org.mifosx.openbanking.feature.sendmoney.SendMoneyScreen
import org.mifosx.openbanking.feature.settings.settingsDestination
import org.mifosx.openbanking.feature.standingorders.CreateStandingOrderScreen
import org.mifosx.openbanking.feature.standingorders.StandingOrdersScreen
import org.mifosx.openbanking.feature.transactions.TransactionsScreen
import org.mifosx.openbanking.placeholder.AccountDetailRoute
import org.mifosx.openbanking.placeholder.AccountsRoute
import org.mifosx.openbanking.placeholder.AtmLocatorRoute
import org.mifosx.openbanking.placeholder.BeneficiariesRoute
import org.mifosx.openbanking.placeholder.CardDetailRoute
import org.mifosx.openbanking.placeholder.CardsRoute
import org.mifosx.openbanking.placeholder.FoDashboardRoute
import org.mifosx.openbanking.placeholder.SendMoneyAmountRoute
import org.mifosx.openbanking.placeholder.SendMoneyConfirmRoute
import org.mifosx.openbanking.placeholder.SendMoneyRoute
import org.mifosx.openbanking.placeholder.StandingOrderEditRoute
import org.mifosx.openbanking.placeholder.StandingOrdersRoute
import org.mifosx.openbanking.placeholder.TransactionDetailRoute
import org.mifosx.openbanking.placeholder.TransactionsRoute
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
        // Respect the system bars (status bar / control center + display cutouts); the bottom
        // navigation bar inset is handled by the scaffold's bottomBar. Previously zero,
        // which let tab content draw under the status bar.
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
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
                // Tab targets switch tabs (navigateToTab) — navigate(route) would push another
                // tab's destination onto the Home stack, breaking the Home tab button.
                onTransfer = { navController.navigateToTab(AuthenticatedNavBarTabItem.PayTab) },
                onAccounts = { navController.navigateToTab(AuthenticatedNavBarTabItem.AccountsTab) },
                onViewCards = { navController.navigateToTab(AuthenticatedNavBarTabItem.CardsTab) },
                // Non-tab destinations are genuine pushes.
                onStandingOrders = { navController.navigate(StandingOrdersRoute) },
                onFindAtm = { navController.navigate(AtmLocatorRoute) },
                onBeneficiaries = { navController.navigate(BeneficiariesRoute) },
                onDeferred = { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message = message, duration = Short)
                    }
                },
            )
            profileDestination(
                onBackClick = navController::popBackStack,
            )
            settingsDestination(
                onBackClick = navController::popBackStack,
                onNavigateToProfile = { navController.navigateToProfile() },
            )

            // Accounts tab — real feature module (Phase 5). Detail + request-account targets
            // remain Phase 2 placeholders until their own feature modules land.
            composableWithStayTransitions<AccountsRoute> {
                AccountsScreen(
                    onAccountClick = { bankId, accountId ->
                        navController.navigate(AccountDetailRoute(bankId = bankId, accountId = accountId))
                    },
                )
            }

            composableWithStayTransitions<AccountDetailRoute> { entry ->
                val route = entry.toRoute<AccountDetailRoute>()
                AccountDetailScreen(
                    bankId = route.bankId,
                    accountId = route.accountId,
                    onBack = navController::popBackStack,
                    onViewTransactions = {
                        navController.navigate(
                            TransactionsRoute(bankId = route.bankId, accountId = route.accountId),
                        )
                    },
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

            // Pay tab — Send Money hub → amount entry → confirm (rail-routed).
            sendMoneyDestinations(navController, scope, snackbarHostState)

            // Beneficiaries — real feature module. Tapping a beneficiary heads to the Pay tab.
            composableWithStayTransitions<BeneficiariesRoute> {
                BeneficiariesScreen(
                    // Pop Beneficiaries off the back stack BEFORE switching tabs, so returning
                    // to the Home tab restores the dashboard (not Beneficiaries) and the Home
                    // nav button works in one tap.
                    onBeneficiaryClick = {
                        navController.popBackStack()
                        navController.navigateToTab(AuthenticatedNavBarTabItem.PayTab)
                    },
                    onBack = navController::popBackStack,
                )
            }

            // Standing orders — real feature module. Rows derive from transaction history
            // (OBP has no read endpoint); creation is its own screen posting the real endpoint.
            standingOrdersDestinations(navController)

            // Transaction history — real feature module. Booked rows + pending (INITIATED)
            // payments for one account; row taps land on the transaction-detail placeholder.
            composableWithStayTransitions<TransactionsRoute> { entry ->
                val route = entry.toRoute<TransactionsRoute>()
                TransactionsScreen(
                    bankId = route.bankId,
                    accountId = route.accountId,
                    onTransactionClick = { navController.navigate(TransactionDetailRoute) },
                    onBack = navController::popBackStack,
                )
            }

            // All other banking destinations (Phase 2 placeholders → real in Phases 4–6).
            bankingPlaceholderDestinations()
        }
    }
}

private fun NavGraphBuilder.sendMoneyDestinations(
    navController: NavHostController,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
) {
    // Pay tab — Send Money hub. Pick a recipient/beneficiary → amount entry preselected;
    // New / Local transfer → amount entry with the beneficiary picker.
    composableWithStayTransitions<SendMoneyRoute> {
        SendMoneyHubScreen(
            onRecipientSelected = { accountId, id ->
                navController.navigate(SendMoneyAmountRoute(counterpartyId = id, accountId = accountId))
            },
            onNewTransfer = { accountId ->
                navController.navigate(SendMoneyAmountRoute(accountId = accountId))
            },
            onBack = navController::popBackStack,
        )
    }

    // Amount entry. Continue carries the validated draft to confirm as type-safe args.
    composableWithStayTransitions<SendMoneyAmountRoute> { entry ->
        val a = entry.toRoute<SendMoneyAmountRoute>()
        SendMoneyScreen(
            onContinue = { d ->
                navController.navigate(
                    SendMoneyConfirmRoute(
                        fromBankId = d.fromBankId,
                        fromAccountId = d.fromAccountId,
                        fromLabel = d.fromLabel,
                        amount = d.amount,
                        currency = d.currency,
                        counterpartyId = d.counterpartyId,
                        beneficiaryName = d.beneficiaryName,
                        beneficiaryBank = d.beneficiaryBank,
                        iban = d.iban,
                        reference = d.reference,
                        paymentType = d.paymentType.name,
                        conversionNote = d.conversionNote.orEmpty(),
                    ),
                )
            },
            onBack = navController::popBackStack,
            preselectedCounterpartyId = a.counterpartyId,
            accountId = a.accountId,
        )
    }

    // Confirm Payment — executes the SEPA transfer. On success, clear back to the form,
    // switch to Home, and surface a snackbar; Edit returns to the form.
    composableWithStayTransitions<SendMoneyConfirmRoute> { entry ->
        val r = entry.toRoute<SendMoneyConfirmRoute>()
        SendMoneyConfirmScreen(
            fromBankId = r.fromBankId,
            fromAccountId = r.fromAccountId,
            fromLabel = r.fromLabel,
            amount = r.amount,
            currency = r.currency,
            counterpartyId = r.counterpartyId,
            beneficiaryName = r.beneficiaryName,
            beneficiaryBank = r.beneficiaryBank,
            iban = r.iban,
            reference = r.reference,
            paymentType = r.paymentType,
            conversionNote = r.conversionNote,
            onSuccess = { message ->
                navController.popBackStack(SendMoneyRoute, inclusive = false)
                navController.navigateToTab(AuthenticatedNavBarTabItem.HomeTab)
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
            onEdit = navController::popBackStack,
            onBack = navController::popBackStack,
        )
    }
}

private fun NavGraphBuilder.standingOrdersDestinations(navController: NavHostController) {
    composableWithStayTransitions<StandingOrdersRoute> {
        StandingOrdersScreen(
            onBack = navController::popBackStack,
            onCreate = { accountId -> navController.navigate(StandingOrderEditRoute(accountId)) },
        )
    }
    composableWithStayTransitions<StandingOrderEditRoute> { entry ->
        val route = entry.toRoute<StandingOrderEditRoute>()
        CreateStandingOrderScreen(
            accountId = route.accountId,
            onBack = navController::popBackStack,
            onCreated = navController::popBackStack,
        )
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
