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

package org.mifosx.openbanking.feature.transactions

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Account-scoped transactions route. Reached from the Accounts list (tap an account) as a single
 * pushed screen — no nested graph, since it is not a bottom-nav tab. The [accountId] scopes the OBIE
 * `GET /accounts/{AccountId}/transactions` request.
 */
@Serializable
data class TransactionsRoute(val accountId: String)

fun NavController.navigateToTransactions(accountId: String, navOptions: NavOptions? = null) {
    navigate(TransactionsRoute(accountId), navOptions)
}

/** Registers the transactions screen as a single destination in the host graph. */
fun NavGraphBuilder.transactionsScreen(
    onNavigateToTransactionDetail: (transactionId: String, accountId: String) -> Unit,
    onBack: () -> Unit,
) {
    composableWithStayTransitions<TransactionsRoute> {
        TransactionsScreen(
            onNavigateToTransactionDetail = onNavigateToTransactionDetail,
            onBack = onBack,
        )
    }
}
