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

package org.mifosx.openbanking.feature.transactiondetail

import androidx.navigation.NavGraphBuilder
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Account-scoped, transaction-scoped transaction-detail route. Reached from a transactions-list row as
 * a single pushed screen — not a bottom-nav tab, so no nested graph.
 *
 * Both property names are the navigation argument keys: type-safe navigation serializes them into the
 * `SavedStateHandle` under exactly these names, which
 * [org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailViewModel.TRANSACTION_ID_ARG]
 * and [org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailViewModel.ACCOUNT_ID_ARG]
 * read.
 */
@Serializable
data class TransactionDetailRoute(val transactionId: String, val accountId: String)

/** Registers the transaction-detail screen in the host graph. */
fun NavGraphBuilder.transactionDetailScreen(onBack: () -> Unit) {
    composableWithStayTransitions<TransactionDetailRoute> {
        TransactionDetailScreen(onBack = onBack)
    }
}
