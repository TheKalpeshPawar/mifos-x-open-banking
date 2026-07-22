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

package org.mifosx.openbanking.feature.statementdetail

import androidx.navigation.NavGraphBuilder
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Account-scoped, statement-scoped statement-detail route. Reached from a statements-list row as a
 * single pushed screen — not a bottom-nav tab, so no nested graph.
 *
 * Both property names are the navigation argument keys: type-safe navigation serializes them into the
 * `SavedStateHandle` under exactly these names, which
 * [org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailViewModel.ACCOUNT_ID_ARG] and
 * [org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailViewModel.STATEMENT_ID_ARG] read.
 */
@Serializable
data class StatementDetailRoute(val accountId: String, val statementId: String)

/** Registers the statement-detail screen in the host graph. */
fun NavGraphBuilder.statementDetailScreen(
    onBack: () -> Unit,
    onNavigateToTransactionDetail: (transactionId: String, accountId: String) -> Unit,
) {
    composableWithStayTransitions<StatementDetailRoute> {
        StatementDetailScreen(
            onBack = onBack,
            onNavigateToTransactionDetail = onNavigateToTransactionDetail,
        )
    }
}
