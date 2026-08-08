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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_already_sent_body
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_already_sent_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_approved_detail
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_approved_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_check_again
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_checking
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_checking_hint
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_confirming_funds
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_declined_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_done
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_code_expired
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_insufficient_funds
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_network
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_no_pending
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_no_staged_payment
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_rejected
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_request_rejected
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_response_unreadable
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_state_mismatch
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_submission_failed
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_submission_unconfirmed
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_timed_out
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_error_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_exchanging
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_expired_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_no_connection_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_no_money_moved
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_nothing_pending_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_rejected_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_restart
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_screen_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_submitting
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_submitting_warning
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_unconfirmed_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_unreadable_title
import org.mifosx.openbanking.feature.paymentconsent.generated.resources.feature_payment_consent_validating
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentAction
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentErrorDetail
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
 * The authorisation return leg, and the screen that finishes the payment.
 *
 * There is no navigation icon and no back affordance while the payment is in flight: this screen
 * runs the funds check and the submission, and leaving mid-submission would strand the customer
 * with an instruction whose outcome the app has not yet learned.
 *
 * `KptScaffold`'s `pullToRefreshState` is deliberately left at its default, which disables the
 * gesture. Three reasons, so this does not get "fixed" later: the gesture is driven by nested-scroll
 * events and every state here is a centred, non-scrolling panel, so wiring it would install an
 * indicator that never moves; the only refreshable moment — [PaymentConsentUiState.Checking] once
 * the bank has said "not yet" — already has an explicit **Check again** button, which is the only
 * affordance that works on desktop and web too; and during
 * [PaymentConsentUiState.Submitting] a pull must not re-trigger anything at all, so the gesture
 * would have to appear and vanish mid-flow.
 */
@Composable
internal fun PaymentConsentScreen(
    onPaymentSubmitted: (String) -> Unit,
    onRestartAuthorisation: () -> Unit,
    onAbandoned: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentConsentViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            is PaymentConsentEvent.PaymentSubmitted -> onPaymentSubmitted(event.paymentId)
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
        PaymentConsentUiState.Validating -> ReturningState(
            stateTag = PaymentConsentTestTags.VALIDATING_STATE,
            detail = Res.string.feature_payment_consent_validating,
            modifier = modifier,
        )

        PaymentConsentUiState.Exchanging -> ReturningState(
            stateTag = PaymentConsentTestTags.EXCHANGING_STATE,
            detail = Res.string.feature_payment_consent_exchanging,
            modifier = modifier,
        )

        is PaymentConsentUiState.Checking -> CheckingState(
            canCheckAgain = current.canCheckAgain,
            onCheckAgain = { onAction(PaymentConsentAction.CheckAgain) },
            modifier = modifier,
        )

        PaymentConsentUiState.Approved -> ApprovedState(modifier)

        PaymentConsentUiState.AlreadySubmitted -> AlreadySubmittedState(
            onDone = { onAction(PaymentConsentAction.AbandonPayment) },
            modifier = modifier,
        )

        PaymentConsentUiState.ConfirmingFunds -> ReturningState(
            stateTag = PaymentConsentTestTags.CONFIRMING_FUNDS_STATE,
            detail = Res.string.feature_payment_consent_confirming_funds,
            modifier = modifier,
        )

        PaymentConsentUiState.Submitting -> SubmittingState(modifier)

        is PaymentConsentUiState.Error -> OutcomeState(
            kind = current.kind,
            detail = current.detail,
            onRestart = { onAction(PaymentConsentAction.RetryAuthorisation) },
            onAbandon = { onAction(PaymentConsentAction.AbandonPayment) },
            modifier = modifier,
        )
    }
}

/**
 * The one visual for the whole return leg.
 *
 * Validating, exchanging, checking, confirming funds and submitting are the same picture — a spinner
 * over a line of copy — because they are the same thing to the customer: waiting. What separates
 * them is [detail], which names the stage, and [stateTag], which is what lets a screenshot suite
 * prove it captured the stage it meant to rather than whichever one happened to be on screen.
 */
@Composable
private fun ReturningState(
    stateTag: String,
    detail: StringResource,
    modifier: Modifier = Modifier,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(stateTag),
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(IndicatorSize)
                .testTag(PaymentConsentTestTags.PROGRESS_INDICATOR),
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
        extraContent()
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
    ReturningState(
        stateTag = PaymentConsentTestTags.CHECKING_STATE,
        detail = Res.string.feature_payment_consent_checking,
        modifier = modifier,
    ) {
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

/**
 * The only positive state this screen shows.
 *
 * A submitted payment leaves immediately for its receipt, so without this the screen would go from
 * "waiting for your bank" straight to "checking the money is available" and never once confirm that
 * the approval the customer just gave at the bank actually arrived.
 */
@Composable
private fun ApprovedState(modifier: Modifier = Modifier) {
    val title = stringResource(Res.string.feature_payment_consent_approved_title)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(PaymentConsentTestTags.AUTHORISED_STATE)
            .semantics { contentDescription = title },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconWell(
            icon = Icons.Filled.CheckCircle,
            container = MaterialTheme.colorScheme.primaryContainer,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(PaymentConsentTestTags.OUTCOME_TITLE),
        )
        Text(
            text = stringResource(Res.string.feature_payment_consent_approved_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(PaymentConsentTestTags.OUTCOME_BODY),
        )
    }
}

/**
 * The bank had already created this payment.
 *
 * Neutral rather than celebratory, and neutral rather than alarming: nothing went wrong, but the app
 * cannot show a receipt because the status read returns a status and not a payment id, so it points
 * the customer at their payment list instead of pretending to more certainty than it has.
 *
 * Note what is missing and why: no "No money has been moved" line, because money almost certainly
 * has, and no "Authorise again", because this is the single strongest duplicate-payment risk on the
 * screen — the one case where the app *knows* a payment exists.
 */
@Composable
private fun AlreadySubmittedState(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(Res.string.feature_payment_consent_already_sent_title)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(PaymentConsentTestTags.ALREADY_SUBMITTED_STATE)
            .semantics { contentDescription = title },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconWell(
            icon = Icons.Filled.CheckCircle,
            container = MaterialTheme.colorScheme.surfaceVariant,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(PaymentConsentTestTags.OUTCOME_TITLE),
        )
        Text(
            text = stringResource(Res.string.feature_payment_consent_already_sent_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(PaymentConsentTestTags.OUTCOME_BODY),
        )
        MifosFilledPillButton(
            label = stringResource(Res.string.feature_payment_consent_done),
            onClick = onDone,
            modifier = Modifier.padding(top = ButtonTopGap),
            testTag = PaymentConsentTestTags.ABANDON_BUTTON,
        )
    }
}

/**
 * The one stage with a warning attached.
 *
 * Once the instruction is with the bank its outcome is unknown to the app until the response
 * arrives, so the customer is asked to wait rather than left to guess whether backing out would
 * cancel anything. It would not.
 */
@Composable
private fun SubmittingState(modifier: Modifier = Modifier) {
    ReturningState(
        stateTag = PaymentConsentTestTags.SUBMITTING_STATE,
        detail = Res.string.feature_payment_consent_submitting,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(Res.string.feature_payment_consent_submitting_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(PaymentConsentTestTags.SUBMITTING_WARNING),
        )
    }
}

/**
 * Every way this screen can end badly.
 *
 * Declined, expired and connection-failed are designed states in their own right rather than three
 * captions on one error page: they differ in whose fault it was and in what the customer should do
 * next, so they differ in icon, in tone and in tag. The rest share the generic panel.
 *
 * Abandon is offered alongside restart on every failure: a stuck authorisation should have a clean
 * exit rather than leaving someone to back out of a payment flow and wonder whether it went through.
 */
@Composable
private fun OutcomeState(
    kind: PaymentConsentErrorKind,
    onRestart: () -> Unit,
    onAbandon: () -> Unit,
    modifier: Modifier = Modifier,
    detail: PaymentConsentErrorDetail? = null,
) {
    val visual = kind.visual()
    val title = stringResource(visual.title)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(visual.stateTag)
            .semantics { contentDescription = title },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconWell(
            icon = visual.icon,
            container = if (visual.blameworthy) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            tint = if (visual.blameworthy) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(PaymentConsentTestTags.OUTCOME_TITLE),
        )
        Text(
            text = detail?.message ?: stringResource(visual.body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(PaymentConsentTestTags.OUTCOME_BODY),
        )
        detail?.reference()?.let { reference ->
            Text(
                text = reference,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = BodyMaxWidth)
                    .testTag(PaymentConsentTestTags.OUTCOME_DETAIL),
            )
        }
        if (visual.reassures) {
            Text(
                text = stringResource(Res.string.feature_payment_consent_no_money_moved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = BodyMaxWidth)
                    .testTag(PaymentConsentTestTags.NO_MONEY_MOVED),
            )
        }
        if (visual.retryable) {
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
        } else {
            // The exit is promoted to primary rather than sitting under a greyed-out retry: an
            // action that must not be taken should not be on screen at all, disabled or otherwise.
            MifosFilledPillButton(
                label = stringResource(Res.string.feature_payment_consent_done),
                onClick = onAbandon,
                modifier = Modifier.padding(top = ButtonTopGap),
                testTag = PaymentConsentTestTags.ABANDON_BUTTON,
            )
        }
    }
}

@Composable
private fun IconWell(
    icon: ImageVector,
    container: Color,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(IconWellSize)
            .padding(bottom = IconBottomGap)
            .background(color = container, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(IconSize),
        )
    }
}

/**
 * How one failure renders.
 *
 * @param blameworthy whether the panel wears the error palette. Expiry, a timeout and a dropped
 *   connection are nobody's mistake, and painting them red tells the customer something went wrong
 *   with *them*.
 * @param reassures whether "No money has been moved" is shown. True for every outcome that can
 *   honestly claim it, which is all of them bar
 *   [PaymentConsentErrorKind.SubmissionFailed] — once the instruction is with the bank the app
 *   cannot promise it was not taken, and its own copy asks the customer to check instead.
 * @param retryable whether "Authorise again" is offered at all. False where a one-tap re-run would
 *   invite a payment the customer may not want to make twice:
 *   [PaymentConsentErrorKind.SubmissionFailed], where the bank may already have taken it and the
 *   body copy says to go and check, and
 *   [PaymentConsentErrorKind.NoPendingAuthorisation], where nothing was in flight so there is
 *   nothing to authorise again. Both then show a single neutral exit instead. Note this is about
 *   what the screen *invites*, not about safety: `RetryAuthorisation` discards the session and
 *   returns to the form, so it never replays an instruction. It just should not be the loudest
 *   control on a screen that has told the customer their money might already be gone.
 */
private data class OutcomeVisual(
    val stateTag: String,
    val icon: ImageVector,
    val title: StringResource,
    val body: StringResource,
    val blameworthy: Boolean = true,
    val reassures: Boolean = true,
    val retryable: Boolean = true,
)

private fun PaymentConsentErrorKind.visual(): OutcomeVisual = when (this) {
    PaymentConsentErrorKind.StateMismatch -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.ERROR_STATE,
        icon = Icons.Filled.ErrorOutline,
        title = Res.string.feature_payment_consent_error_title,
        body = Res.string.feature_payment_consent_error_state_mismatch,
    )

    PaymentConsentErrorKind.NoPendingAuthorisation -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.ERROR_STATE,
        icon = Icons.Filled.Info,
        title = Res.string.feature_payment_consent_nothing_pending_title,
        body = Res.string.feature_payment_consent_error_no_pending,
        blameworthy = false,
        retryable = false,
    )

    PaymentConsentErrorKind.CodeExpired -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.EXPIRED_STATE,
        icon = Icons.Filled.HourglassEmpty,
        title = Res.string.feature_payment_consent_expired_title,
        body = Res.string.feature_payment_consent_error_code_expired,
        blameworthy = false,
    )

    PaymentConsentErrorKind.AuthorisationTimedOut -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.EXPIRED_STATE,
        icon = Icons.Filled.HourglassEmpty,
        title = Res.string.feature_payment_consent_expired_title,
        body = Res.string.feature_payment_consent_error_timed_out,
        blameworthy = false,
    )

    PaymentConsentErrorKind.ConsentRejected -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.DECLINED_STATE,
        icon = Icons.Filled.Cancel,
        title = Res.string.feature_payment_consent_declined_title,
        body = Res.string.feature_payment_consent_error_rejected,
    )

    PaymentConsentErrorKind.NetworkError -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.CONNECTION_FAILED_STATE,
        icon = Icons.Filled.CloudOff,
        title = Res.string.feature_payment_consent_no_connection_title,
        body = Res.string.feature_payment_consent_error_network,
        blameworthy = false,
    )

    PaymentConsentErrorKind.NoStagedPayment -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.ERROR_STATE,
        icon = Icons.Filled.ErrorOutline,
        title = Res.string.feature_payment_consent_error_title,
        body = Res.string.feature_payment_consent_error_no_staged_payment,
    )

    PaymentConsentErrorKind.InsufficientFunds -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.ERROR_STATE,
        icon = Icons.Filled.ErrorOutline,
        title = Res.string.feature_payment_consent_error_title,
        body = Res.string.feature_payment_consent_error_insufficient_funds,
    )

    PaymentConsentErrorKind.SubmissionFailed -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.ERROR_STATE,
        icon = Icons.Filled.ErrorOutline,
        title = Res.string.feature_payment_consent_unconfirmed_title,
        body = Res.string.feature_payment_consent_error_submission_failed,
        reassures = false,
        retryable = false,
    )

    PaymentConsentErrorKind.RequestRejected -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.ERROR_STATE,
        icon = Icons.Filled.ErrorOutline,
        title = Res.string.feature_payment_consent_rejected_title,
        body = Res.string.feature_payment_consent_error_request_rejected,
        retryable = false,
    )

    PaymentConsentErrorKind.ResponseUnreadable -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.ERROR_STATE,
        icon = Icons.Filled.ErrorOutline,
        title = Res.string.feature_payment_consent_unreadable_title,
        body = Res.string.feature_payment_consent_error_response_unreadable,
        blameworthy = false,
        retryable = false,
    )

    PaymentConsentErrorKind.SubmissionUnconfirmed -> OutcomeVisual(
        stateTag = PaymentConsentTestTags.ERROR_STATE,
        icon = Icons.Filled.ErrorOutline,
        title = Res.string.feature_payment_consent_unconfirmed_title,
        body = Res.string.feature_payment_consent_error_submission_unconfirmed,
        reassures = false,
        retryable = false,
    )
}

/**
 * The bank's code and its own support reference, on one line under the message.
 *
 * Shown rather than only logged: while this integration is being proven a code like `U027` is the
 * fastest route to the cause, and the reference is the only thing a support call can quote.
 */
private fun PaymentConsentErrorDetail.reference(): String? =
    listOfNotNull(code, supportReference).takeIf { it.isNotEmpty() }?.joinToString(" · ")
