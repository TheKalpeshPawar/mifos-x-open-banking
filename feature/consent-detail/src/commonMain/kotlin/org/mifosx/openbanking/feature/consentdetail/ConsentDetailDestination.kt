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

package org.mifosx.openbanking.feature.consentdetail

import androidx.navigation.NavGraphBuilder
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Consent-detail route. Reached from a consent-list card as a single pushed screen — not a
 * bottom-nav tab, so no nested graph.
 *
 * The [consentId] property name is the navigation argument key: type-safe navigation serializes it
 * into the `SavedStateHandle` under exactly this name, which
 * [org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailViewModel.CONSENT_ID_ARG] reads.
 */
@Serializable
data class ConsentDetailRoute(val consentId: String)

/**
 * Registers the consent-detail screen in the host graph.
 *
 * [onReconfirm] starts a fresh consent flow. [onLoggedOut] fires after a revoke — a full logout — for
 * the host to route to onboarding.
 */
fun NavGraphBuilder.consentDetailScreen(
    onBack: () -> Unit,
    onReconfirm: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    composableWithStayTransitions<ConsentDetailRoute> {
        ConsentDetailScreen(
            onBack = onBack,
            onReconfirm = onReconfirm,
            onLoggedOut = onLoggedOut,
        )
    }
}
