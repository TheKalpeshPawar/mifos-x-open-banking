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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAction
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyErrorKind
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One case per interactive surface: every control this screen renders must dispatch the action its
 * `on_click` contract declares, or navigate. A control that renders but dispatches nothing is the
 * defect this suite exists to catch.
 */
@OptIn(ExperimentalTestApi::class)
class SendMoneyActionTest {

    /**
     * [scroll] is opt-in because only the form states sit in a scroller — the submitting, success
     * and error states fill the viewport, and `performScrollTo` fails without a scrollable ancestor.
     */
    private fun captureActions(
        state: SendMoneyState,
        tag: String,
        scroll: Boolean = false,
    ): List<SendMoneyAction> {
        val actions = mutableListOf<SendMoneyAction>()
        runComposeUiTest {
            setContent {
                SendMoneyScreenContent(
                    state = state,
                    onAction = { actions += it },
                    onNavigateToPaymentStatus = {},
                    onNavigateToConsents = {},
                )
            }
            val node = onNodeWithTag(tag)
            if (scroll) node.performScrollTo()
            node.performClick()
        }
        return actions
    }

    @Test
    fun tappingAnAccountRowSelectsThatPayer() {
        val actions = captureActions(
            SendMoneyFixtures.recipientState(),
            SendMoneyTestTags.debtorRow(SendMoneyFixtures.SAVINGS_ACCOUNT_ID),
        )

        assertEquals(
            listOf(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.SAVINGS_ACCOUNT_ID)),
            actions,
        )
    }

    @Test
    fun tappingAPayeeRowSelectsThatCreditor() {
        val actions = captureActions(
            SendMoneyFixtures.recipientState(),
            SendMoneyTestTags.creditorRow(SendMoneyFixtures.JAMESON_ID),
        )

        assertEquals(listOf(SendMoneyAction.SelectCreditor(SendMoneyFixtures.JAMESON_ID)), actions)
    }

    @Test
    fun tappingManualEntryOpensTheFields() {
        val actions = captureActions(
            SendMoneyFixtures.recipientState(),
            SendMoneyTestTags.MANUAL_ENTRY_BUTTON,
        )

        assertEquals(listOf(SendMoneyAction.ShowManualCreditorEntry), actions)
    }

    @Test
    fun tappingUseTheseDetailsConfirmsTheManualPayee() {
        val actions = captureActions(
            SendMoneyFixtures.recipientState(manualEntryVisible = true),
            SendMoneyTestTags.MANUAL_CONFIRM,
            scroll = true,
        )

        assertEquals(listOf(SendMoneyAction.ConfirmManualCreditor), actions)
    }

    @Test
    fun tappingReviewMovesToTheReviewStep() {
        val actions = captureActions(SendMoneyFixtures.amountState(), SendMoneyTestTags.REVIEW_BUTTON, scroll = true)

        assertEquals(listOf(SendMoneyAction.ReviewPayment), actions)
    }

    @Test
    fun tappingConfirmStagesTheConsent() {
        val actions = captureActions(SendMoneyFixtures.reviewState(), SendMoneyTestTags.CONFIRM_BUTTON, scroll = true)

        assertEquals(listOf(SendMoneyAction.ConfirmAndStageConsent), actions)
    }

    @Test
    fun tappingCancelAbandonsThePayment() {
        val actions = captureActions(SendMoneyFixtures.reviewState(), SendMoneyTestTags.CANCEL_BUTTON, scroll = true)

        assertEquals(listOf(SendMoneyAction.CancelPayment), actions)
    }

    @Test
    fun tappingRetryResubmits() {
        val actions = captureActions(
            SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.NetworkError),
            SendMoneyTestTags.RETRY_BUTTON,
        )

        assertEquals(listOf(SendMoneyAction.RetrySubmit), actions)
    }

    @Test
    fun tappingChangeAmountGoesBackAStep() {
        val actions = captureActions(
            SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.OutsideControlParameters),
            SendMoneyTestTags.EDIT_AMOUNT_BUTTON,
        )

        assertEquals(listOf(SendMoneyAction.BackStep), actions)
    }

    @Test
    fun tappingAuthoriseAgainRestagesTheConsent() {
        val actions = captureActions(
            SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.ConsentNotAuthorised),
            SendMoneyTestTags.REAUTHORISE_BUTTON,
        )

        assertEquals(listOf(SendMoneyAction.ConfirmAndStageConsent), actions)
    }

    /** The two navigating controls are callbacks, not actions — the host owns the route table. */
    @Test
    fun trackThisPaymentNavigatesRatherThanDispatching() {
        val actions = mutableListOf<SendMoneyAction>()
        var trackedPaymentId: String? = null
        runComposeUiTest {
            setContent {
                SendMoneyScreenContent(
                    state = SendMoneyFixtures.successState(),
                    onAction = { actions += it },
                    onNavigateToPaymentStatus = { trackedPaymentId = it },
                    onNavigateToConsents = {},
                )
            }
            onNodeWithTag(SendMoneyTestTags.VIEW_PAYMENT_STATUS_BUTTON).performClick()
        }

        assertEquals(SendMoneyFixtures.PAYMENT_ID, trackedPaymentId)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun viewConsentsNavigatesRatherThanDispatching() {
        val actions = mutableListOf<SendMoneyAction>()
        var wentToConsents = false
        runComposeUiTest {
            setContent {
                SendMoneyScreenContent(
                    state = SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.ConsentRevoked),
                    onAction = { actions += it },
                    onNavigateToPaymentStatus = {},
                    onNavigateToConsents = { wentToConsents = true },
                )
            }
            onNodeWithTag(SendMoneyTestTags.VIEW_CONSENTS_BUTTON).performClick()
        }

        assertTrue(wentToConsents)
        assertTrue(actions.isEmpty())
    }
}
