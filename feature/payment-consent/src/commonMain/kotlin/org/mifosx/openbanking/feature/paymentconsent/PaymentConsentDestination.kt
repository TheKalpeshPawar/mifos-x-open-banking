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

package org.mifosx.openbanking.feature.paymentconsent

import androidx.navigation.NavGraphBuilder
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * @property redirectUrl HSBC's raw redirect, forwarded unparsed. Parsing and authenticating it are
 *   the data layer's business, not the route's. The property name is the `SavedStateHandle` key.
 */
@Serializable
data class PaymentConsentRoute(val redirectUrl: String)

fun NavGraphBuilder.paymentConsentScreen(
    onAuthorised: (String) -> Unit,
    onRestartAuthorisation: () -> Unit,
    onAbandoned: () -> Unit,
) {
    composableWithStayTransitions<PaymentConsentRoute> {
        PaymentConsentScreen(
            onAuthorised = onAuthorised,
            onRestartAuthorisation = onRestartAuthorisation,
            onAbandoned = onAbandoned,
        )
    }
}
