/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:Suppress("MatchingDeclarationName", "TooManyFunctions")

package org.mifosx.openbanking.placeholder

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/*
 * Type-safe route table for all 44 idea-layer screens (Phase 2 stub).
 *
 * The 6 screens that already own a real destination — home, profile, settings,
 * login, splash, notifications — are wired by their own modules. The remaining
 * 38 below are placeholder destinations: type-safe @Serializable routes rendering
 * [PlaceholderScreen], replaced by real feature modules in Phases 4–6. Detail
 * routes are arg-less data objects for now; their id/key args are added when the
 * owning feature is implemented.
 */

@Serializable data object AccountsRoute

@Serializable data object AccountDetailRoute

@Serializable data object TransactionsRoute

@Serializable data object TransactionDetailRoute

@Serializable data object TransactionTagsRoute

@Serializable data object SendMoneyRoute

@Serializable data object SendMoneyConfirmRoute

@Serializable data object BeneficiariesRoute

@Serializable data object CardsRoute

@Serializable data object CardDetailRoute

@Serializable data object StandingOrdersRoute

@Serializable data object StandingOrderDetailRoute

@Serializable data object StandingOrderEditRoute

@Serializable data object DirectDebitsRoute

@Serializable data object DirectDebitDetailRoute

@Serializable data object PfmDashboardRoute

@Serializable data object FxRatesRoute

@Serializable data object AtmLocatorRoute

@Serializable data object ProductsRoute

@Serializable data object ConsentManagerRoute

@Serializable data object FoDashboardRoute

@Serializable data object CustomerSearchRoute

@Serializable data object CustomerDetailRoute

@Serializable data object CustomerProfileRoute

@Serializable data object CustomerOnboardingRoute

@Serializable data object CorporateOnboardingRoute

@Serializable data object KycReviewRoute

@Serializable data object AccountApplicationsRoute

@Serializable data object ApplicationDetailRoute

@Serializable data object AgentRegistrationRoute

@Serializable data object MeetingsRoute

@Serializable data object CustomerMessagesRoute

@Serializable data object ChangePasswordRoute

@Serializable data object AboutRoute

@Serializable data object TermsOfServiceRoute

@Serializable data object PrivacyPolicyRoute

@Serializable data object LicensesRoute

@Serializable data object ForgotPasswordRoute

fun NavController.navigateToAccounts(navOptions: NavOptions? = null) = navigate(AccountsRoute, navOptions)
fun NavController.navigateToSendMoney(navOptions: NavOptions? = null) = navigate(SendMoneyRoute, navOptions)
fun NavController.navigateToBeneficiaries(navOptions: NavOptions? = null) = navigate(BeneficiariesRoute, navOptions)
fun NavController.navigateToCards(navOptions: NavOptions? = null) = navigate(CardsRoute, navOptions)
fun NavController.navigateToFoDashboard(navOptions: NavOptions? = null) = navigate(FoDashboardRoute, navOptions)
fun NavController.navigateToCustomerSearch(navOptions: NavOptions? = null) = navigate(CustomerSearchRoute, navOptions)
fun NavController.navigateToAccountApplications(navOptions: NavOptions? = null) =
    navigate(AccountApplicationsRoute, navOptions)
fun NavController.navigateToCustomerMessages(navOptions: NavOptions? = null) =
    navigate(CustomerMessagesRoute, navOptions)

/**
 * Registers empty composables for all 38 not-yet-built banking screens. Wired
 * into the authenticated nav host so every route resolves end to end in Phase 2.
 */
fun NavGraphBuilder.bankingPlaceholderDestinations() {
    composableWithStayTransitions<AccountDetailRoute> { PlaceholderScreen("Account detail") }
    composableWithStayTransitions<TransactionsRoute> { PlaceholderScreen("Transactions") }
    composableWithStayTransitions<TransactionDetailRoute> { PlaceholderScreen("Transaction detail") }
    composableWithStayTransitions<TransactionTagsRoute> { PlaceholderScreen("Transaction tags") }
    composableWithStayTransitions<SendMoneyRoute> { PlaceholderScreen("Send money") }
    composableWithStayTransitions<SendMoneyConfirmRoute> { PlaceholderScreen("Confirm payment") }
    // BeneficiariesRoute is a real feature module — registered in AuthenticatedNavbarNavigationScreen.
    composableWithStayTransitions<CardDetailRoute> { PlaceholderScreen("Card detail") }
    composableWithStayTransitions<StandingOrdersRoute> { PlaceholderScreen("Standing orders") }
    composableWithStayTransitions<StandingOrderDetailRoute> { PlaceholderScreen("Standing order detail") }
    composableWithStayTransitions<StandingOrderEditRoute> { PlaceholderScreen("Edit standing order") }
    composableWithStayTransitions<DirectDebitsRoute> { PlaceholderScreen("Direct debits") }
    composableWithStayTransitions<DirectDebitDetailRoute> { PlaceholderScreen("Direct debit detail") }
    composableWithStayTransitions<PfmDashboardRoute> { PlaceholderScreen("Insights") }
    composableWithStayTransitions<FxRatesRoute> { PlaceholderScreen("FX rates") }
    composableWithStayTransitions<AtmLocatorRoute> { PlaceholderScreen("ATM locator") }
    composableWithStayTransitions<ProductsRoute> { PlaceholderScreen("Products") }
    composableWithStayTransitions<ConsentManagerRoute> { PlaceholderScreen("Consent manager") }

    composableWithStayTransitions<FoDashboardRoute> { PlaceholderScreen("Dashboard") }
    composableWithStayTransitions<CustomerSearchRoute> { PlaceholderScreen("Customers") }
    composableWithStayTransitions<CustomerDetailRoute> { PlaceholderScreen("Customer detail") }
    composableWithStayTransitions<CustomerProfileRoute> { PlaceholderScreen("Customer profile") }
    composableWithStayTransitions<CustomerOnboardingRoute> { PlaceholderScreen("Customer onboarding") }
    composableWithStayTransitions<CorporateOnboardingRoute> { PlaceholderScreen("Corporate onboarding") }
    composableWithStayTransitions<KycReviewRoute> { PlaceholderScreen("KYC review") }
    composableWithStayTransitions<AccountApplicationsRoute> { PlaceholderScreen("Applications") }
    composableWithStayTransitions<ApplicationDetailRoute> { PlaceholderScreen("Application detail") }
    composableWithStayTransitions<AgentRegistrationRoute> { PlaceholderScreen("Agent registration") }
    composableWithStayTransitions<MeetingsRoute> { PlaceholderScreen("Meetings") }
    composableWithStayTransitions<CustomerMessagesRoute> { PlaceholderScreen("Messages") }

    composableWithStayTransitions<ChangePasswordRoute> { PlaceholderScreen("Change password") }
    composableWithStayTransitions<AboutRoute> { PlaceholderScreen("About") }
    composableWithStayTransitions<TermsOfServiceRoute> { PlaceholderScreen("Terms of service") }
    composableWithStayTransitions<PrivacyPolicyRoute> { PlaceholderScreen("Privacy policy") }
    composableWithStayTransitions<LicensesRoute> { PlaceholderScreen("Licenses") }
    composableWithStayTransitions<ForgotPasswordRoute> { PlaceholderScreen("Forgot password") }
}
