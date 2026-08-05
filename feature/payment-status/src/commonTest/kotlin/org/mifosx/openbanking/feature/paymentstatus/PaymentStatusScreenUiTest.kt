/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.core.model.banking.payment.PaymentDisposition
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PaymentStatusScreenUiTest {

    @Test
    fun loadingRendersTheSkeleton() = runComposeUiTest {
        setContent {
            PaymentStatusScreenContent(PaymentStatusFixtures.loadingState(), {}, {})
        }
        onNodeWithTag(PaymentStatusTestTags.SKELETON).assertIsDisplayed()
        onNodeWithTag(PaymentStatusTestTags.SUMMARY_CARD).assertDoesNotExist()
    }

    @Test
    fun contentRendersTheSummaryAndEveryDetailRow() = runComposeUiTest {
        setContent {
            PaymentStatusScreenContent(PaymentStatusFixtures.contentState(), {}, {})
        }
        onNodeWithTag(PaymentStatusTestTags.SUMMARY_CARD).assertIsDisplayed()
        onNodeWithTag(PaymentStatusTestTags.AMOUNT).assertIsDisplayed()
        onNodeWithTag(PaymentStatusTestTags.STATUS_CHIP).assertIsDisplayed()
        onNodeWithTag(PaymentStatusTestTags.DETAIL_REFERENCE).performScrollTo().assertIsDisplayed()
        onNodeWithTag(PaymentStatusTestTags.DETAIL_FROM).performScrollTo().assertIsDisplayed()
        onNodeWithTag(PaymentStatusTestTags.DETAIL_SUBMITTED).performScrollTo().assertIsDisplayed()
        onNodeWithTag(PaymentStatusTestTags.DETAIL_PAYMENT_ID).performScrollTo().assertIsDisplayed()
    }

    /** The note is the whole reason an in-flight payment does not read as a failure. */
    @Test
    fun anInFlightPaymentExplainsItself() = runComposeUiTest {
        setContent {
            PaymentStatusScreenContent(PaymentStatusFixtures.contentState(), {}, {})
        }
        onNodeWithTag(PaymentStatusTestTags.IN_PROGRESS_NOTE).assertIsDisplayed()
    }

    @Test
    fun aSettledPaymentDropsTheInFlightNote() = runComposeUiTest {
        setContent {
            PaymentStatusScreenContent(
                PaymentStatusFixtures.contentState(disposition = PaymentDisposition.TerminalSuccess),
                {},
                {},
            )
        }
        onNodeWithTag(PaymentStatusTestTags.IN_PROGRESS_NOTE).assertDoesNotExist()
    }

    /** A second refresh while one is running would be a duplicate read for no benefit. */
    @Test
    fun refreshIsDisabledWhileARefreshIsRunning() = runComposeUiTest {
        setContent {
            PaymentStatusScreenContent(PaymentStatusFixtures.contentState(refreshing = true), {}, {})
        }
        onNodeWithTag(PaymentStatusTestTags.REFRESH_BUTTON).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun tappingRefreshRereadsTheStatus() {
        val actions = mutableListOf<PaymentStatusAction>()
        runComposeUiTest {
            setContent {
                PaymentStatusScreenContent(
                    state = PaymentStatusFixtures.contentState(),
                    onAction = { actions += it },
                    onStartNewPayment = {},
                )
            }
            onNodeWithTag(PaymentStatusTestTags.REFRESH_BUTTON).performScrollTo().performClick()
        }

        assertEquals<List<PaymentStatusAction>>(listOf(PaymentStatusAction.RefreshStatus), actions)
    }

    /** Starting another payment is navigation, so it must not dispatch an action. */
    @Test
    fun tappingMakeAnotherPaymentNavigates() {
        val actions = mutableListOf<PaymentStatusAction>()
        var started = false
        runComposeUiTest {
            setContent {
                PaymentStatusScreenContent(
                    state = PaymentStatusFixtures.contentState(),
                    onAction = { actions += it },
                    onStartNewPayment = { started = true },
                )
            }
            onNodeWithTag(PaymentStatusTestTags.NEW_PAYMENT_BUTTON).performScrollTo().performClick()
        }

        assertTrue(started)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun errorRendersRetry() = runComposeUiTest {
        setContent {
            PaymentStatusScreenContent(PaymentStatusFixtures.errorState(), {}, {})
        }
        onNodeWithTag(PaymentStatusTestTags.ERROR_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentStatusTestTags.RETRY_BUTTON).assertIsDisplayed()
    }

    @Test
    fun tappingRetryRereadsTheStatus() {
        val actions = mutableListOf<PaymentStatusAction>()
        runComposeUiTest {
            setContent {
                PaymentStatusScreenContent(
                    state = PaymentStatusFixtures.errorState(),
                    onAction = { actions += it },
                    onStartNewPayment = {},
                )
            }
            onNodeWithTag(PaymentStatusTestTags.RETRY_BUTTON).performClick()
        }

        assertEquals<List<PaymentStatusAction>>(listOf(PaymentStatusAction.RefreshStatus), actions)
    }
}
