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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentAction
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PaymentConsentScreenUiTest {

    private fun captureActions(state: PaymentConsentState, tag: String): List<PaymentConsentAction> {
        val actions = mutableListOf<PaymentConsentAction>()
        runComposeUiTest {
            setContent {
                PaymentConsentScreenContent(state = state, onAction = { actions += it })
            }
            onNodeWithTag(tag).performClick()
        }
        return actions
    }

    @Test
    fun validatingRendersProgressWithItsOwnDetail() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.validatingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL).assertIsDisplayed()
    }

    @Test
    fun exchangingRendersProgress() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.exchangingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertIsDisplayed()
    }

    /** While the first poll is in flight there is nothing useful to offer yet. */
    @Test
    fun checkingOffersNothingUntilTheFirstPollComesBack() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.checkingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).assertDoesNotExist()
    }

    /** Once the bank has said "not yet", explain the wait and offer another look. */
    @Test
    fun aConsentStillAwaitingAuthorisationExplainsTheWait() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(PaymentConsentFixtures.checkingState(canCheckAgain = true), {})
        }
        onNodeWithTag(PaymentConsentTestTags.CHECKING_HINT).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).assertIsDisplayed()
    }

    @Test
    fun tappingCheckAgainRepolls() {
        val actions = captureActions(
            PaymentConsentFixtures.checkingState(canCheckAgain = true),
            PaymentConsentTestTags.CHECK_AGAIN_BUTTON,
        )

        assertEquals<List<PaymentConsentAction>>(listOf(PaymentConsentAction.CheckAgain), actions)
    }

    @Test
    fun authorisedRendersItsOwnState() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.authorisedState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.AUTHORISED_STATE).assertIsDisplayed()
    }

    /** Both exits are always present: restarting and abandoning are equally legitimate here. */
    @Test
    fun errorOffersBothRestartAndAbandon() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.errorState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.ERROR_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertIsDisplayed()
    }

    @Test
    fun tappingAuthoriseAgainRestarts() {
        val actions = captureActions(
            PaymentConsentFixtures.errorState(),
            PaymentConsentTestTags.RESTART_BUTTON,
        )

        assertEquals<List<PaymentConsentAction>>(
            listOf(PaymentConsentAction.RetryAuthorisation),
            actions,
        )
    }

    @Test
    fun tappingCancelAbandonsThePayment() {
        val actions = captureActions(
            PaymentConsentFixtures.errorState(),
            PaymentConsentTestTags.ABANDON_BUTTON,
        )

        assertEquals<List<PaymentConsentAction>>(listOf(PaymentConsentAction.AbandonPayment), actions)
    }
}
