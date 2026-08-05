/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentconsent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.components.MifosFilledPillButton
import org.mifosx.openbanking.core.ui.components.MifosTonalPillButton
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.Res
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_abandon
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_authorised_body
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_authorised_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_check_again
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_checking
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_checking_hint
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_code_expired
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_network
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_no_pending
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_rejected
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_state_mismatch
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_timed_out
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_exchanging
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_restart
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_screen_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_validating
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentAction
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentErrorKind
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentEvent
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentState
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentUiState
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentViewModel
import template.core.base.ui.effects.EventsEffect

private val ContentPadding = 24.dp
private val LineGap = 16.dp
private val IndicatorSize = 56.dp
private val IconWellSize = 96.dp
private val IconSize = 44.dp
private val IconBottomGap = 8.dp
private val BodyMaxWidth = 320.dp
private val ButtonTopGap = 8.dp

/**
 * The authorisation return leg.
 *
 * Every outcome leaves as a callback rather than a route change — send-money holds the idempotency
 * key and the staged instruction, so it is the only place that can decide what happens next.
 */
@Composable
internal fun PaymentConsentScreen(
    onAuthorised: (String) -> Unit,
    onRestartAuthorisation: () -> Unit,
    onAbandoned: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentConsentViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            is PaymentConsentEvent.Authorised -> onAuthorised(event.consentId)
            PaymentConsentEvent.RestartAuthorisation -> onRestartAuthorisation()
            PaymentConsentEvent.Abandoned -> onAbandoned()
        }
    }

    KptScaffold(
        showNavigationIcon = false,
        title = stringResource(Res.string.feature_payment_consent_screen_title),
        modifier = modifier,
    ) {
        PaymentConsentScreenContent(state = state, onAction = viewModel::trySendAction)
    }
}

/** The stateless half every UI suite drives directly. */
@Composable
internal fun PaymentConsentScreenContent(
    state: PaymentConsentState,
    onAction: (PaymentConsentAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        PaymentConsentUiState.Validating ->
            ProgressState(Res.string.feature_payment_consent_validating, modifier)

        PaymentConsentUiState.Exchanging ->
            ProgressState(Res.string.feature_payment_consent_exchanging, modifier)

        is PaymentConsentUiState.Checking -> CheckingState(
            canCheckAgain = current.canCheckAgain,
            onCheckAgain = { onAction(PaymentConsentAction.CheckAgain) },
            modifier = modifier,
        )

        PaymentConsentUiState.Authorised -> AuthorisedState(modifier)

        is PaymentConsentUiState.Error -> ErrorState(
            kind = current.kind,
            onRestart = { onAction(PaymentConsentAction.RetryAuthorisation) },
            onAbandon = { onAction(PaymentConsentAction.AbandonPayment) },
            modifier = modifier,
        )
    }
}

@Composable
private fun ProgressState(detail: StringResource, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(PaymentConsentTestTags.PROGRESS_INDICATOR),
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(IndicatorSize),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(PaymentConsentTestTags.PROGRESS_DETAIL),
        )
    }
}

/**
 * A bank that has not confirmed yet is a normal outcome, so this explains the wait and offers
 * another look rather than spinning until it times out into an error nobody can act on.
 */
@Composable
private fun CheckingState(
    canCheckAgain: Boolean,
    onCheckAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(PaymentConsentTestTags.PROGRESS_INDICATOR),
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(IndicatorSize),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.feature_payment_consent_checking),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(PaymentConsentTestTags.PROGRESS_DETAIL),
        )
        if (canCheckAgain) {
            Text(
                text = stringResource(Res.string.feature_payment_consent_checking_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = BodyMaxWidth)
                    .testTag(PaymentConsentTestTags.CHECKING_HINT),
            )
            MifosTonalPillButton(
                label = stringResource(Res.string.feature_payment_consent_check_again),
                onClick = onCheckAgain,
                modifier = Modifier.padding(top = ButtonTopGap),
                testTag = PaymentConsentTestTags.CHECK_AGAIN_BUTTON,
            )
        }
    }
}

@Composable
private fun AuthorisedState(modifier: Modifier = Modifier) {
    val title = stringResource(Res.string.feature_payment_consent_authorised_title)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(PaymentConsentTestTags.AUTHORISED_STATE)
            .semantics { contentDescription = title },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconWellSize)
                .padding(bottom = IconBottomGap)
                .background(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.feature_payment_consent_authorised_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = BodyMaxWidth),
        )
    }
}

/**
 * Abandon is offered alongside restart on every failure: a stuck authorisation should have a clean
 * exit rather than leaving someone to back out of a payment flow and wonder whether it went through.
 */
@Composable
private fun ErrorState(
    kind: PaymentConsentErrorKind,
    onRestart: () -> Unit,
    onAbandon: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.feature_payment_consent_error_title)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(PaymentConsentTestTags.ERROR_STATE)
            .semantics { contentDescription = title },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconWellSize)
                .padding(bottom = IconBottomGap)
                .background(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(kind.bodyResource()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = BodyMaxWidth),
        )
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_payment_consent_restart),
            onClick = onRestart,
            modifier = Modifier.padding(top = ButtonTopGap),
            testTag = PaymentConsentTestTags.RESTART_BUTTON,
        )
        MifosTonalPillButton(
            label = stringResource(Res.string.feature_payment_consent_abandon),
            onClick = onAbandon,
            testTag = PaymentConsentTestTags.ABANDON_BUTTON,
        )
    }
}

private fun PaymentConsentErrorKind.bodyResource(): StringResource = when (this) {
    PaymentConsentErrorKind.StateMismatch -> Res.string.feature_payment_consent_error_state_mismatch
    PaymentConsentErrorKind.NoPendingAuthorisation -> Res.string.feature_payment_consent_error_no_pending
    PaymentConsentErrorKind.CodeExpired -> Res.string.feature_payment_consent_error_code_expired
    PaymentConsentErrorKind.ConsentRejected -> Res.string.feature_payment_consent_error_rejected
    PaymentConsentErrorKind.AuthorisationTimedOut -> Res.string.feature_payment_consent_error_timed_out
    PaymentConsentErrorKind.NetworkError -> Res.string.feature_payment_consent_error_network
}
