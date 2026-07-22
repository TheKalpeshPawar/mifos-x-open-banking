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

package org.mifosx.openbanking.feature.scheduledpayments

import androidx.navigation.NavGraphBuilder
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Account-scoped scheduled-payments route. Reached from the account-detail Scheduled chip as a
 * single pushed screen — not a bottom-nav tab, so no nested graph.
 *
 * The [accountId] property name is the navigation argument key: type-safe navigation serializes it
 * into the `SavedStateHandle` under exactly this name, which
 * [org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsViewModel.ACCOUNT_ID_ARG]
 * reads.
 */
@Serializable
data class ScheduledPaymentsRoute(val accountId: String)

/** Registers the scheduled-payments screen in the host graph. */
fun NavGraphBuilder.scheduledPaymentsScreen(onBack: () -> Unit) {
    composableWithStayTransitions<ScheduledPaymentsRoute> {
        ScheduledPaymentsScreen(onBack = onBack)
    }
}
