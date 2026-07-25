/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.product

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.product.generated.resources.Res
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_title
import org.mifosx.openbanking.feature.product.ui.ProductAction
import org.mifosx.openbanking.feature.product.ui.ProductState
import org.mifosx.openbanking.feature.product.ui.ProductUiState
import org.mifosx.openbanking.feature.product.ui.ProductViewModel

/**
 * Product-terms screen: the fees, credit-interest tiers, overdraft rates and features published for one
 * account.
 *
 * A pushed account-scoped screen with a back arrow and no bottom navigation, like its siblings — the
 * mockup draws a bottom bar, but in this app the bar belongs to tab start destinations only. Account
 * detail is reached from the Accounts tab, and this screen is entered from account detail's Product
 * option.
 */
@Composable
internal fun ProductScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_product_title),
        modifier = modifier,
    ) {
        ProductScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
        )
    }
}

/**
 * Stateless renderer for the four product states, kept separate from [ProductScreen] so the UI suites can
 * drive each state without standing up a view model or Koin.
 */
@Composable
internal fun ProductScreenContent(
    state: ProductState,
    onAction: (ProductAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val uiState = state.uiState) {
        ProductUiState.Loading -> ProductSkeleton(modifier = modifier)

        is ProductUiState.Content -> ProductContent(product = uiState.product, modifier = modifier)

        ProductUiState.Empty -> ProductEmpty(modifier = modifier)

        is ProductUiState.Error -> ProductError(
            kind = uiState.kind,
            onRetry = { onAction(ProductAction.RetryLoad) },
            modifier = modifier,
        )
    }
}
