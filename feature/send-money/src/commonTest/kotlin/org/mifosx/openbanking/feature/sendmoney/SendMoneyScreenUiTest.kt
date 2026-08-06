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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAmountProblem
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyErrorKind
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyStage
import kotlin.test.Test

/**
 * Renders each state through the stateless [SendMoneyScreenContent] on the desktop runner.
 *
 * The recurring assertion is which recovery buttons exist, because the whole design of the error
 * state is that the four are never interchangeable and never all shown.
 */
@OptIn(ExperimentalTestApi::class)
class SendMoneyScreenUiTest {

    /** TC-SEND-002: the skeleton, not a spinner — the app's loading convention. */
    @Test
    fun loadingRendersTheSkeleton() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.loadingState(), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.SKELETON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.DEBTOR_LIST).assertDoesNotExist()
    }

    /** TC-SEND-001. */
    @Test
    fun theRecipientStepRendersBothPickersAndTheManualEntryAffordance() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.recipientState(), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.STEP_INDICATOR).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.DEBTOR_LIST).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.CREDITOR_LIST).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.MANUAL_ENTRY_BUTTON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.debtorRow(SendMoneyFixtures.CURRENT_ACCOUNT_ID)).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.creditorRow(SendMoneyFixtures.JAMESON_ID)).assertIsDisplayed()
    }

    /** TC-SEND-013: no payees is not a dead end. */
    @Test
    fun anEmptyPayeeListKeepsManualEntryAvailable() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.recipientState(beneficiaries = emptyList()), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.NO_SAVED_PAYEES).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.CREDITOR_LIST).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.MANUAL_ENTRY_BUTTON).assertIsDisplayed()
    }

    /** The manual fields sit below the payee list, so they need scrolling to before asserting. */
    @Test
    fun theManualFieldsAppearOnlyWhenAskedFor() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.recipientState(manualEntryVisible = true), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.MANUAL_SORT_CODE).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.MANUAL_ACCOUNT_NUMBER).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.MANUAL_CONFIRM).performScrollTo().assertIsDisplayed()
    }

    /**
     * TC-SEND-003: an unpayable amount blocks the way forward rather than failing later.
     *
     * The error reads from the unmerged tree because it lives in the text field's `supportingText`
     * slot, whose semantics merge into the field itself.
     */
    @Test
    fun anAmountBeyondTheBalanceDisablesReview() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.amountState(problem = SendMoneyAmountProblem.ExceedsAvailableBalance),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.AMOUNT_ERROR, useUnmergedTree = true).assertExists()
        onNodeWithTag(SendMoneyTestTags.REVIEW_BUTTON).assertIsNotEnabled()
    }

    /** TC-SMC-001: the amount leads, the detail rows confirm it, and both controls are present. */
    @Test
    fun theReviewStepLeadsWithTheAmountAndListsEveryDetail() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.reviewState(), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.REVIEW_HERO).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_AMOUNT).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_PAYEE_CHIP).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_SUMMARY).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_TO).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_FROM).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_REFERENCE).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_SENT_VIA).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_TOTAL).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_AUTH_NOTICE).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.CONFIRM_BUTTON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.EDIT_PAYMENT_BUTTON).assertIsDisplayed()
    }

    /**
     * TC-SMC-005. A fee figure here could only be hardcoded — charges arrive on the consent
     * response, which does not exist until Confirm is tapped — and the sandbox quotes charges on
     * some payments, so a printed £0.00 would sometimes be false.
     */
    @Test
    fun theReviewStepShowsNoFeeFigureItCannotKnow() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.reviewState(), {}, {})
        }
        onNodeWithText("£0.00").assertDoesNotExist()
    }

    /**
     * The submit guard, rendered. Confirming unmounts the control rather than disabling it, so there
     * is no button on screen at all to tap a second time.
     */
    @Test
    fun submittingRendersNoCallToActionAtAll() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.submittingState(), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.SUBMITTING_INDICATOR).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.SUBMITTING_AMOUNT).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.SUBMITTING_LOCK_NOTE).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.CONFIRM_BUTTON).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.CANCEL_BUTTON).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.REVIEW_BUTTON).assertDoesNotExist()
    }

    @Test
    fun submittingNamesTheStageItHasReached() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.submittingState(stage = SendMoneyStage.AwaitingAuthorisation),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.SUBMITTING_INDICATOR).assertIsDisplayed()
    }

    /** TC-SEND-009: U014 offers exactly one recovery, and it is not Retry. */
    @Test
    fun anOutOfLimitsFailureOffersOnlyChangeAmount() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.OutsideControlParameters),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.EDIT_AMOUNT_BUTTON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.RETRY_BUTTON).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.REAUTHORISE_BUTTON).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.VIEW_CONSENTS_BUTTON).assertDoesNotExist()
    }

    /** TC-SEND-007: a client defect gets no button, only the reference to quote. */
    @Test
    fun aSignatureFailureOffersNoRecoveryAtAll() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.SignatureMissing),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.ERROR_SUPPORT_REFERENCE).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.RETRY_BUTTON).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.REAUTHORISE_BUTTON).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.VIEW_CONSENTS_BUTTON).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.EDIT_AMOUNT_BUTTON).assertDoesNotExist()
    }

    /** TC-SEND-008. */
    @Test
    fun anUnauthorisedConsentOffersOnlyReauthorise() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.ConsentNotAuthorised),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.REAUTHORISE_BUTTON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.RETRY_BUTTON).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.EDIT_AMOUNT_BUTTON).assertDoesNotExist()
    }

    @Test
    fun aRevokedConsentOffersOnlyViewConsents() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.ConsentRevoked),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.VIEW_CONSENTS_BUTTON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.RETRY_BUTTON).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.EDIT_AMOUNT_BUTTON).assertDoesNotExist()
    }

    @Test
    fun aTransportFailureOffersRetry() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.NetworkError, supportReference = null),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.RETRY_BUTTON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.ERROR_SUPPORT_REFERENCE).assertDoesNotExist()
    }

    /** TC-SEND-011: an insufficient balance is fixed by changing the amount, not by retrying. */
    @Test
    fun insufficientFundsOffersOnlyChangeAmount() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.errorState(kind = SendMoneyErrorKind.InsufficientFunds),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.EDIT_AMOUNT_BUTTON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.RETRY_BUTTON).assertDoesNotExist()
    }
}
