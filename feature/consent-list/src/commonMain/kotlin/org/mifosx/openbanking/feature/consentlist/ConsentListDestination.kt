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

package org.mifosx.openbanking.feature.consentlist

import androidx.navigation.NavGraphBuilder
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Consent-list route. Reached from Settings, Profile, the Accounts reconfirm prompt and home's
 * Connect action, as a single pushed screen — not a bottom-nav tab, so no nested graph.
 *
 * Argument-less: the consents shown are whatever this device staged, read from the session rather
 * than passed in.
 */
@Serializable
data object ConsentListRoute

/**
 * Registers the consent-list screen in the host graph.
 *
 * Three outbound routes, all raised rather than taken: [onNavigateToDetail] carries the tapped
 * consent's id, [onConnectBank] starts a fresh consent, and [onReauthenticate] recovers an expired
 * PSU session.
 */
fun NavGraphBuilder.consentListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onConnectBank: () -> Unit,
    onReauthenticate: () -> Unit,
) {
    composableWithStayTransitions<ConsentListRoute> {
        ConsentListScreen(
            onBack = onBack,
            onNavigateToDetail = onNavigateToDetail,
            onConnectBank = onConnectBank,
            onReauthenticate = onReauthenticate,
        )
    }
}
