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

package org.mifosx.openbanking.feature.product

import androidx.navigation.NavGraphBuilder
import kotlinx.serialization.Serializable
import template.core.base.ui.nav.composableWithStayTransitions

/**
 * Route for the product-terms screen.
 *
 * The `accountId` property name **is** the navigation argument key — type-safe navigation serialises it
 * into the `SavedStateHandle` under exactly this name, and `ProductViewModel.ACCOUNT_ID_ARG` reads it
 * back. Renaming the property without renaming that constant yields a blank id and a screen that fails
 * silently, with no compile error; `ProductViewModelTest` pins the pair with a serial-descriptor
 * assertion.
 */
@Serializable
data class ProductRoute(val accountId: String)

fun NavGraphBuilder.productScreen(onBack: () -> Unit) {
    composableWithStayTransitions<ProductRoute> {
        ProductScreen(onBack = onBack)
    }
}
