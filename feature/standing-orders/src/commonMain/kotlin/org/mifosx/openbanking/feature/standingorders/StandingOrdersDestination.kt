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

package org.mifosx.openbanking.feature.standingorders

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Account-scoped standing-orders route. Reached from the account-detail Explore row as a single
 * pushed screen — not a bottom-nav tab, so no nested graph.
 *
 * The [accountId] property name is the navigation argument key: type-safe navigation serializes it
 * into the `SavedStateHandle` under exactly this name, which
 * [org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersViewModel.ACCOUNT_ID_ARG] reads.
 */
@Serializable
data class StandingOrdersRoute(val accountId: String)

fun NavController.navigateToStandingOrders(accountId: String, navOptions: NavOptions? = null) {
    navigate(StandingOrdersRoute(accountId), navOptions)
}

/** Registers the standing-orders screen in the host graph. */
fun NavGraphBuilder.standingOrdersScreen(onBack: () -> Unit) {
    composableWithStayTransitions<StandingOrdersRoute> {
        StandingOrdersScreen(onBack = onBack)
    }
}
