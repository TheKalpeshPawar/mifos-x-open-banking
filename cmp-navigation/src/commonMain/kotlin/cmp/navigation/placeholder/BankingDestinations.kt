/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
@file:Suppress("MatchingDeclarationName", "TooManyFunctions")

package cmp.navigation.placeholder

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/*
 * Type-safe route table for the consumer AISP idea-layer screens (Phase 2 stub).
 *
 * The 6 screens that already own a real destination — home, profile, settings,
 * login, splash, notifications — are wired by their own modules. The remaining
 * 26 below are placeholder destinations: type-safe @Serializable routes rendering
 * [PlaceholderScreen], replaced by real feature modules in Phases 4–6. Detail
 * routes are arg-less data objects for now; their id/key args are added when the
 * owning feature is implemented.
 */

// ─── Consumer ────────────────────────────────────────────────────────────────
@Serializable data object TransactionsRoute

@Serializable data object TransactionTagsRoute

@Serializable data object SendMoneyRoute

@Serializable data object SendMoneyConfirmRoute

@Serializable data object CardsRoute

@Serializable data object CardDetailRoute

@Serializable data object StandingOrderDetailRoute

@Serializable data object StandingOrderEditRoute

@Serializable data object PartyRoute

@Serializable data object DirectDebitDetailRoute

@Serializable data object PfmDashboardRoute

@Serializable data object FxRatesRoute

@Serializable data object AtmLocatorRoute

@Serializable data object ProductsRoute

// ─── Shared (non-tab) ────────────────────────────────────────────────────────
@Serializable data object ChangePasswordRoute

@Serializable data object AboutRoute

@Serializable data object TermsOfServiceRoute

@Serializable data object PrivacyPolicyRoute

@Serializable data object LicensesRoute

@Serializable data object ForgotPasswordRoute

// ─── Bottom-nav navigation helpers ───────────────────────────────────────────
fun NavController.navigateToSendMoney(navOptions: NavOptions? = null) = navigate(SendMoneyRoute, navOptions)
fun NavController.navigateToCards(navOptions: NavOptions? = null) = navigate(CardsRoute, navOptions)

/**
 * Registers empty composables for all 26 not-yet-built banking screens. Wired
 * into the authenticated nav host so every route resolves end to end in Phase 2.
 */
fun NavGraphBuilder.bankingPlaceholderDestinations() {
    composableWithStayTransitions<TransactionsRoute> { PlaceholderScreen("Transactions") }
    composableWithStayTransitions<TransactionTagsRoute> { PlaceholderScreen("Transaction tags") }
    composableWithStayTransitions<SendMoneyRoute> { PlaceholderScreen("Send money") }
    composableWithStayTransitions<SendMoneyConfirmRoute> { PlaceholderScreen("Confirm payment") }
    composableWithStayTransitions<CardsRoute> { PlaceholderScreen("Cards") }
    composableWithStayTransitions<CardDetailRoute> { PlaceholderScreen("Card detail") }
    composableWithStayTransitions<StandingOrderDetailRoute> { PlaceholderScreen("Standing order detail") }
    composableWithStayTransitions<StandingOrderEditRoute> { PlaceholderScreen("Edit standing order") }
    composableWithStayTransitions<PartyRoute> { PlaceholderScreen("Account holder") }
    composableWithStayTransitions<DirectDebitDetailRoute> { PlaceholderScreen("Direct debit detail") }
    composableWithStayTransitions<PfmDashboardRoute> { PlaceholderScreen("Insights") }
    composableWithStayTransitions<FxRatesRoute> { PlaceholderScreen("FX rates") }
    composableWithStayTransitions<AtmLocatorRoute> { PlaceholderScreen("ATM locator") }
    composableWithStayTransitions<ProductsRoute> { PlaceholderScreen("Products") }

    composableWithStayTransitions<ChangePasswordRoute> { PlaceholderScreen("Change password") }
    composableWithStayTransitions<AboutRoute> { PlaceholderScreen("About") }
    composableWithStayTransitions<TermsOfServiceRoute> { PlaceholderScreen("Terms of service") }
    composableWithStayTransitions<PrivacyPolicyRoute> { PlaceholderScreen("Privacy policy") }
    composableWithStayTransitions<LicensesRoute> { PlaceholderScreen("Licenses") }
    composableWithStayTransitions<ForgotPasswordRoute> { PlaceholderScreen("Forgot password") }
}
