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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.ui.NavigationItem
import org.mifosx.openbanking.feature.accounts.AccountDetailScreen
import org.mifosx.openbanking.feature.accounts.AccountsScreen
import org.mifosx.openbanking.feature.atmlocator.AtmLocatorScreen
import org.mifosx.openbanking.feature.atmlocator.platform.isAtmLocatorSupported
import org.mifosx.openbanking.feature.beneficiaries.BeneficiariesScreen
import org.mifosx.openbanking.feature.businessinsights.BusinessInsightsScreen
import org.mifosx.openbanking.feature.cards.CardsScreen
import org.mifosx.openbanking.feature.directdebits.DirectDebitDetailScreen
import org.mifosx.openbanking.feature.directdebits.DirectDebitsScreen
import org.mifosx.openbanking.feature.fxrates.FxRatesScreen
import org.mifosx.openbanking.feature.home.HomeDestination
import org.mifosx.openbanking.feature.home.homeGraph
import org.mifosx.openbanking.feature.legal.AboutScreen
import org.mifosx.openbanking.feature.legal.LicensesScreen
import org.mifosx.openbanking.feature.legal.PrivacyPolicyScreen
import org.mifosx.openbanking.feature.legal.TermsOfServiceScreen
import org.mifosx.openbanking.feature.pfm.PfmDashboardScreen
import org.mifosx.openbanking.feature.pfm.settings.PfmSettingsScreen
import org.mifosx.openbanking.feature.products.ProductsScreen
import org.mifosx.openbanking.feature.profile.navigateToProfile
import org.mifosx.openbanking.feature.profile.profileDestination
import org.mifosx.openbanking.feature.sendmoney.PaymentResultScreen
import org.mifosx.openbanking.feature.sendmoney.ScaChallengeScreen
import org.mifosx.openbanking.feature.sendmoney.SendMoneyConfirmScreen
import org.mifosx.openbanking.feature.sendmoney.SendMoneyHubScreen
import org.mifosx.openbanking.feature.sendmoney.SendMoneyScreen
import org.mifosx.openbanking.feature.sendmoney.ui.SandboxTanDestination
import org.mifosx.openbanking.feature.settings.settingsDestination
import org.mifosx.openbanking.feature.standingorders.CreateStandingOrderScreen
import org.mifosx.openbanking.feature.standingorders.StandingOrderDetailScreen
import org.mifosx.openbanking.feature.standingorders.StandingOrdersScreen
import org.mifosx.openbanking.feature.transactions.TransactionDetailScreen
import org.mifosx.openbanking.feature.transactions.TransactionTagsScreen
import org.mifosx.openbanking.feature.transactions.TransactionsScreen
import org.mifosx.openbanking.placeholder.AboutRoute
import org.mifosx.openbanking.placeholder.AccountDetailRoute
import org.mifosx.openbanking.placeholder.AccountsRoute
import org.mifosx.openbanking.placeholder.AtmLocatorRoute
import org.mifosx.openbanking.placeholder.BeneficiariesRoute
import org.mifosx.openbanking.placeholder.BusinessInsightsRoute
import org.mifosx.openbanking.placeholder.CardDetailRoute
import org.mifosx.openbanking.placeholder.CardsRoute
import org.mifosx.openbanking.placeholder.DirectDebitDetailRoute
import org.mifosx.openbanking.placeholder.DirectDebitsRoute
import org.mifosx.openbanking.placeholder.FoDashboardRoute
import org.mifosx.openbanking.placeholder.FxRatesRoute
import org.mifosx.openbanking.placeholder.LicensesRoute
import org.mifosx.openbanking.placeholder.PaymentResultRoute
import org.mifosx.openbanking.placeholder.PfmDashboardRoute
import org.mifosx.openbanking.placeholder.PfmSettingsRoute
import org.mifosx.openbanking.placeholder.PrivacyPolicyRoute
import org.mifosx.openbanking.placeholder.ProductsRoute
import org.mifosx.openbanking.placeholder.ScaChallengeRoute
import org.mifosx.openbanking.placeholder.SendMoneyAmountRoute
import org.mifosx.openbanking.placeholder.SendMoneyConfirmRoute
import org.mifosx.openbanking.placeholder.SendMoneyRoute
import org.mifosx.openbanking.placeholder.StandingOrderDetailRoute
import org.mifosx.openbanking.placeholder.StandingOrderEditRoute
import org.mifosx.openbanking.placeholder.StandingOrdersRoute
import org.mifosx.openbanking.placeholder.TermsOfServiceRoute
import org.mifosx.openbanking.placeholder.TransactionDetailRoute
import org.mifosx.openbanking.placeholder.TransactionTagsRoute
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
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    val message = stringResource(Res.string.not_connected)
    LaunchedEffect(isOffline) {
        if (isOffline) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = Indefinite,
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
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
                onAccounts = { navController.navigateToTab(AuthenticatedNavBarTabItem.AccountsTab) },
                // Non-tab destinations are genuine pushes.
                onStandingOrders = { navController.navigate(StandingOrdersRoute) },
                onFindAtm = { navController.navigate(AtmLocatorRoute) },
                onInsights = { navController.navigate(PfmDashboardRoute) },
                onBusinessInsights = { navController.navigate(BusinessInsightsRoute) },
                onFxRates = { navController.navigate(FxRatesRoute) },
                onProducts = { navController.navigate(ProductsRoute) },
                showFindAtm = isAtmLocatorSupported(),
            )
            profileDestination(
                onBackClick = navController::popBackStack,
            )
            settingsDestination(
                onBackClick = navController::popBackStack,
                onNavigateToProfile = { navController.navigateToProfile() },
                onNavigateToAbout = { navController.navigate(AboutRoute) },
                onNavigateToTerms = { navController.navigate(TermsOfServiceRoute) },
                onNavigateToPrivacy = { navController.navigate(PrivacyPolicyRoute) },
                onNavigateToLicenses = { navController.navigate(LicensesRoute) },
            )
            legalDestinations(navController)

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
                    onViewDirectDebits = {
                        navController.navigate(
                            DirectDebitsRoute(bankId = route.bankId, accountId = route.accountId),
                        )
                    },
                )
            }

            // Cards tab — real feature module. Carousel + per-account transactions are live;
            // card controls (freeze/limit/PIN/report/order) surface a "coming soon" snackbar
            // (no consumer OBP endpoint — see CardsViewModel). card-detail remains a Phase 2
            // placeholder until its feature module lands.
            composableWithStayTransitions<CardsRoute> {
                CardsScreen(
                    onCardClick = { navController.navigate(CardDetailRoute) },
                    onDeferred = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                )
            }

            // Pay tab — Send Money hub → amount entry → confirm (rail-routed).
            sendMoneyDestinations(navController)

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

            // Transaction history + detail + tags — real feature modules.
            transactionsDestinations(navController)

            // Spending Insights — real feature module. The account is resolved internally
            // (persisted default → checking-first); merchants and view-all land on the
            // transaction history for the inspected account.
            composableWithStayTransitions<PfmDashboardRoute> {
                PfmDashboardScreen(
                    onViewTransactions = { bankId, accountId ->
                        navController.navigate(TransactionsRoute(bankId = bankId, accountId = accountId))
                    },
                    onOpenSettings = { navController.navigate(PfmSettingsRoute) },
                    onBack = navController::popBackStack,
                )
            }

            composableWithStayTransitions<PfmSettingsRoute> {
                PfmSettingsScreen(onBack = navController::popBackStack)
            }

            composableWithStayTransitions<BusinessInsightsRoute> {
                BusinessInsightsScreen(onBack = navController::popBackStack)
            }

            composableWithStayTransitions<FxRatesRoute> {
                FxRatesScreen(
                    onSendMoney = {
                        navController.popBackStack()
                        navController.navigateToTab(AuthenticatedNavBarTabItem.PayTab)
                    },
                    onBack = navController::popBackStack,
                )
            }

            directDebitsDestinations(navController)

            composableWithStayTransitions<ProductsRoute> {
                ProductsScreen(onBack = navController::popBackStack)
            }

            composableWithStayTransitions<AtmLocatorRoute> {
                AtmLocatorScreen(onBack = navController::popBackStack)
            }

            // All other banking destinations (Phase 2 placeholders → real in Phases 4–6).
            bankingPlaceholderDestinations()
        }
    }
}

/** About / Terms / Privacy / Licenses — static feature/legal screens reached from Settings. */
private fun NavGraphBuilder.legalDestinations(navController: NavHostController) {
    composableWithStayTransitions<AboutRoute> {
        AboutScreen(
            onBack = navController::popBackStack,
            appVersion = koinInject<String>(named("appVersion")).ifBlank { "1.0.0" },
        )
    }
    composableWithStayTransitions<TermsOfServiceRoute> {
        TermsOfServiceScreen(onBack = navController::popBackStack)
    }
    composableWithStayTransitions<PrivacyPolicyRoute> {
        PrivacyPolicyScreen(onBack = navController::popBackStack)
    }
    composableWithStayTransitions<LicensesRoute> {
        LicensesScreen(onBack = navController::popBackStack)
    }
}

private fun NavGraphBuilder.sendMoneyDestinations(
    navController: NavHostController,
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
                        useSandboxTan = d.useSandboxTan,
                        toBankId = d.toBankId,
                        toAccountId = d.toAccountId,
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
            sandboxTan = if (r.useSandboxTan) SandboxTanDestination(r.toBankId, r.toAccountId) else null,
            onCompleted = { request ->
                navController.navigate(
                    paymentResultRoute(
                        request = request,
                        amount = r.amount,
                        currency = r.currency,
                        beneficiaryName = r.beneficiaryName,
                        fromLabel = r.fromLabel,
                        fromBankId = r.fromBankId,
                        fromAccountId = r.fromAccountId,
                    ),
                )
            },
            onChallengeRequired = { args ->
                navController.navigate(
                    ScaChallengeRoute(
                        bankId = args.bankId,
                        accountId = args.accountId,
                        type = args.type,
                        requestId = args.requestId,
                        challengeId = args.challengeId,
                        amount = args.amount,
                        currency = args.currency,
                        beneficiaryName = args.beneficiaryName,
                        fromLabel = args.fromLabel,
                    ),
                )
            },
            onEdit = navController::popBackStack,
            onBack = navController::popBackStack,
        )
    }

    // SCA challenge — answer the one-time code for a payment that returned INITIATED, then show the
    // payment-result screen carrying the original display fields + the booked transaction.
    composableWithStayTransitions<ScaChallengeRoute> { entry ->
        val c = entry.toRoute<ScaChallengeRoute>()
        ScaChallengeScreen(
            bankId = c.bankId,
            accountId = c.accountId,
            type = c.type,
            requestId = c.requestId,
            challengeId = c.challengeId,
            onCompleted = { request ->
                navController.navigate(
                    paymentResultRoute(
                        request = request,
                        amount = c.amount,
                        currency = c.currency,
                        beneficiaryName = c.beneficiaryName,
                        fromLabel = c.fromLabel,
                        fromBankId = c.bankId,
                        fromAccountId = c.accountId,
                    ),
                )
            },
            onBack = navController::popBackStack,
        )
    }

    // Payment result — success summary; "View transaction" opens the booked row, "Done" returns home.
    composableWithStayTransitions<PaymentResultRoute> { entry ->
        val p = entry.toRoute<PaymentResultRoute>()
        PaymentResultScreen(
            amount = p.amount,
            currency = p.currency,
            beneficiaryName = p.beneficiaryName,
            fromLabel = p.fromLabel,
            transactionId = p.transactionId,
            chargeAmount = p.chargeAmount,
            chargeCurrency = p.chargeCurrency,
            status = p.status,
            onViewTransaction = {
                navController.navigate(
                    TransactionDetailRoute(
                        bankId = p.fromBankId,
                        accountId = p.fromAccountId,
                        transactionId = p.transactionId,
                    ),
                )
            },
            onDone = {
                navController.popBackStack(SendMoneyRoute, inclusive = false)
                navController.navigateToTab(AuthenticatedNavBarTabItem.HomeTab)
            },
        )
    }
}

/** Builds the payment-result route from the original display fields + the booked [request]. */
private fun paymentResultRoute(
    request: TransactionRequest,
    amount: String,
    currency: String,
    beneficiaryName: String,
    fromLabel: String,
    fromBankId: String,
    fromAccountId: String,
) = PaymentResultRoute(
    amount = amount,
    currency = currency,
    beneficiaryName = beneficiaryName,
    fromLabel = fromLabel,
    transactionId = request.transactionIds.firstOrNull().orEmpty(),
    chargeAmount = request.charge.value.amount,
    chargeCurrency = request.charge.value.currency,
    status = request.status,
    fromBankId = fromBankId,
    fromAccountId = fromAccountId,
)

private fun NavGraphBuilder.transactionsDestinations(navController: NavHostController) {
    // Transaction history — booked rows + pending (INITIATED) payments for one account;
    // row taps open the transaction-detail screen.
    composableWithStayTransitions<TransactionsRoute> { entry ->
        val route = entry.toRoute<TransactionsRoute>()
        TransactionsScreen(
            bankId = route.bankId,
            accountId = route.accountId,
            onTransactionClick = { transactionId ->
                navController.navigate(
                    TransactionDetailRoute(
                        bankId = route.bankId,
                        accountId = route.accountId,
                        transactionId = transactionId,
                    ),
                )
            },
            onPendingClick = { requestId ->
                navController.navigate(
                    TransactionDetailRoute(
                        bankId = route.bankId,
                        accountId = route.accountId,
                        requestId = requestId,
                    ),
                )
            },
            onBack = navController::popBackStack,
        )
    }

    // Transaction detail — booked rows arrive with transactionId; pending rows with requestId.
    composableWithStayTransitions<TransactionDetailRoute> { entry ->
        val route = entry.toRoute<TransactionDetailRoute>()
        TransactionDetailScreen(
            bankId = route.bankId,
            accountId = route.accountId,
            transactionId = route.transactionId,
            requestId = route.requestId,
            onManageTags = {
                navController.navigate(
                    TransactionTagsRoute(
                        bankId = route.bankId,
                        accountId = route.accountId,
                        transactionId = route.transactionId,
                    ),
                )
            },
            onBack = navController::popBackStack,
        )
    }

    // Tags & Notes — backed by the OBP v1.2.1 transaction-metadata endpoints.
    composableWithStayTransitions<TransactionTagsRoute> { entry ->
        val route = entry.toRoute<TransactionTagsRoute>()
        TransactionTagsScreen(
            bankId = route.bankId,
            accountId = route.accountId,
            transactionId = route.transactionId,
            onBack = navController::popBackStack,
        )
    }
}

private fun NavGraphBuilder.directDebitsDestinations(navController: NavHostController) {
    composableWithStayTransitions<DirectDebitsRoute> { entry ->
        val route = entry.toRoute<DirectDebitsRoute>()
        DirectDebitsScreen(
            bankId = route.bankId,
            accountId = route.accountId,
            onBack = navController::popBackStack,
            onMandateClick = { mandateId ->
                navController.navigate(
                    DirectDebitDetailRoute(
                        bankId = route.bankId,
                        accountId = route.accountId,
                        mandateId = mandateId,
                    ),
                )
            },
        )
    }
    composableWithStayTransitions<DirectDebitDetailRoute> { entry ->
        val route = entry.toRoute<DirectDebitDetailRoute>()
        DirectDebitDetailScreen(
            bankId = route.bankId,
            accountId = route.accountId,
            mandateId = route.mandateId,
            onViewAllPayments = {
                navController.navigate(
                    TransactionsRoute(bankId = route.bankId, accountId = route.accountId),
                )
            },
            onBack = navController::popBackStack,
        )
    }
}

private fun NavGraphBuilder.standingOrdersDestinations(navController: NavHostController) {
    composableWithStayTransitions<StandingOrdersRoute> {
        StandingOrdersScreen(
            onBack = navController::popBackStack,
            onCreate = { accountId -> navController.navigate(StandingOrderEditRoute(accountId)) },
            onOrderClick = { bankId, accountId, standingOrderId ->
                navController.navigate(
                    StandingOrderDetailRoute(
                        bankId = bankId,
                        accountId = accountId,
                        standingOrderId = standingOrderId,
                    ),
                )
            },
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
    // Standing order detail — execution rows drill into the underlying booked transaction.
    composableWithStayTransitions<StandingOrderDetailRoute> { entry ->
        val route = entry.toRoute<StandingOrderDetailRoute>()
        StandingOrderDetailScreen(
            bankId = route.bankId,
            accountId = route.accountId,
            standingOrderId = route.standingOrderId,
            onExecutionClick = { transactionId ->
                navController.navigate(
                    TransactionDetailRoute(
                        bankId = route.bankId,
                        accountId = route.accountId,
                        transactionId = transactionId,
                    ),
                )
            },
            onBack = navController::popBackStack,
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
