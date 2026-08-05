/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_screen_title
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAction
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyEvent
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyState
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyUiState
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyViewModel
import template.core.base.ui.effects.EventsEffect

/**
 * The Pay tab.
 *
 * No navigation icon: this is a bottom-nav root, not a pushed screen, so there is nowhere to go
 * back to.
 *
 * The two events leave the ViewModel rather than becoming state because only the composition can
 * act on them — opening the bank's authorisation page is a browser hop, and moving to payment status
 * is the host's route table.
 */
@Composable
internal fun SendMoneyScreen(
    onLaunchAuthorisation: (String) -> Unit,
    onNavigateToPaymentStatus: (String) -> Unit,
    onNavigateToConsents: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SendMoneyViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            is SendMoneyEvent.LaunchAuthorisation -> onLaunchAuthorisation(event.url)
            is SendMoneyEvent.PaymentSucceeded -> Unit
        }
    }

    KptScaffold(
        showNavigationIcon = false,
        title = stringResource(Res.string.feature_send_money_screen_title),
        modifier = modifier,
    ) {
        SendMoneyScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onNavigateToPaymentStatus = onNavigateToPaymentStatus,
            onNavigateToConsents = onNavigateToConsents,
        )
    }
}

/**
 * The stateless half every UI suite drives directly.
 *
 * `PaymentSucceeded` is not navigated on automatically: the PSU sees the receipt and chooses whether
 * to follow the payment, so the move to payment status is a tap on the success screen rather than
 * something that happens to them.
 */
@Composable
internal fun SendMoneyScreenContent(
    state: SendMoneyState,
    onAction: (SendMoneyAction) -> Unit,
    onNavigateToPaymentStatus: (String) -> Unit,
    onNavigateToConsents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        SendMoneyUiState.Loading -> SendMoneySkeleton(modifier = modifier)

        is SendMoneyUiState.Content -> SendMoneyContent(
            state = current,
            onAction = onAction,
            modifier = modifier,
        )

        is SendMoneyUiState.Submitting -> SendMoneySubmitting(
            stage = current.stage,
            amountLabel = current.amountLabel,
            creditorName = current.creditorName,
            modifier = modifier,
        )

        is SendMoneyUiState.Success -> SendMoneySuccess(
            amountLabel = current.amountLabel,
            creditorName = current.creditorName,
            paymentId = current.paymentId,
            onViewStatus = { onNavigateToPaymentStatus(current.paymentId) },
            modifier = modifier,
        )

        is SendMoneyUiState.Error -> SendMoneyError(
            kind = current.kind,
            supportReference = current.supportReference,
            onRetry = { onAction(SendMoneyAction.RetrySubmit) },
            onReauthorise = { onAction(SendMoneyAction.ConfirmAndStageConsent) },
            onViewConsents = onNavigateToConsents,
            onEditAmount = { onAction(SendMoneyAction.BackStep) },
            modifier = modifier,
        )
    }
}
