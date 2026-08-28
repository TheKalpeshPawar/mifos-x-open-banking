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

package org.mifosx.openbanking.feature.paymentsschedulepayment

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/** The feature's graph route. */
@Serializable
data object SchedulePaymentDestination

@Serializable
data object SchedulePaymentRoute

@Serializable
data object SchedulePaymentHistoryRoute

fun NavGraphBuilder.schedulePaymentGraph(
    onLaunchAuthorisation: (String) -> Unit,
    onNavigateToConsents: () -> Unit,
    onNavigateToPayment: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onBack: () -> Unit,
) {
    navigation<SchedulePaymentDestination>(startDestination = SchedulePaymentRoute) {
        composableWithStayTransitions<SchedulePaymentRoute> {
            SchedulePaymentScreen(
                onLaunchAuthorisation = onLaunchAuthorisation,
                onNavigateToConsents = onNavigateToConsents,
                onNavigateToPayment = onNavigateToPayment,
                onNavigateToHistory = onNavigateToHistory,
                onBack = onBack,
            )
        }
        composableWithStayTransitions<SchedulePaymentHistoryRoute> {
            SchedulePaymentHistoryScreen(
                onBack = onBack,
                onNavigateToPayment = onNavigateToPayment,
            )
        }
    }
}
