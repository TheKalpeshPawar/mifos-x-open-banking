/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
@file:Suppress("MatchingDeclarationName")

package org.mifosx.openbanking.feature.accountdetail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Account-scoped detail route. Reached from the accounts list and from the home hero card as a
 * single pushed screen — not a bottom-nav tab, so no nested graph.
 *
 * The [accountId] property name is the navigation argument key: type-safe navigation serializes it
 * into the `SavedStateHandle` under exactly this name, which
 * [org.mifosx.openbanking.feature.accountdetail.ui.AccountDetailViewModel.ACCOUNT_ID_ARG] reads.
 */
@Serializable
data class AccountDetailRoute(val accountId: String)

fun NavController.navigateToAccountDetail(accountId: String, navOptions: NavOptions? = null) {
    navigate(AccountDetailRoute(accountId), navOptions)
}

/**
 * Registers the account-detail screen in the host graph.
 *
 * [onNavigateToChip] receives the tapped Explore destination together with the current account id,
 * leaving the host to resolve each to its route. Destinations whose features are not built yet
 * resolve to their placeholder routes.
 */
fun NavGraphBuilder.accountDetailScreen(
    onNavigateToChip: (chip: AccountDetailChip, accountId: String) -> Unit,
    onBack: () -> Unit,
) {
    composableWithStayTransitions<AccountDetailRoute> {
        AccountDetailScreen(
            onNavigateToChip = onNavigateToChip,
            onBack = onBack,
        )
    }
}
