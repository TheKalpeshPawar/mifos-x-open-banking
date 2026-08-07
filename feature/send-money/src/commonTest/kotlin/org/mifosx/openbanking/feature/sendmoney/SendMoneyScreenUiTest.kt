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
import org.mifosx.openbanking.core.model.banking.payment.ChargeBearer
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
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
    fun theFormPageRendersBothPickersAndTheManualEntryAffordance() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.formState(), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.DEBTOR_LIST).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.CREDITOR_LIST).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.MANUAL_ENTRY_BUTTON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.debtorRow(SendMoneyFixtures.CURRENT_ACCOUNT_ID)).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.creditorRow(SendMoneyFixtures.JAMESON_ID)).assertIsDisplayed()
    }

    /**
     * The payer, the payee and the amount are one page, not three steps.
     *
     * This is the assertion the split into steps made impossible: the amount used to live behind a
     * transition, so nothing could check that all three are reachable without one.
     */
    @Test
    fun theFormPageCarriesTheAmountAndReferenceOnTheSameScroll() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.formState(), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.FORM_PAGE).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.RAIL_TOGGLE).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.DEBTOR_LIST).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.AMOUNT_FIELD).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REFERENCE_FIELD).performScrollTo().assertIsDisplayed()
    }

    /**
     * The payee list is account-scoped, so before a payer exists there is nothing to show.
     *
     * A notice rather than an empty list: an empty list looks like "you have no payees", which is a
     * different and wrong statement.
     */
    @Test
    fun noPayerChosenExplainsWhyThereAreNoPayees() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.formState(debtorAccountId = null), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.PAYEE_NEEDS_PAYER).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.CREDITOR_LIST).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.NO_SAVED_PAYEES).assertDoesNotExist()
    }

    /** Sending no payer is something to choose, not something left undone. */
    @Test
    fun theBankChoiceIsOfferedAlongsideTheAccounts() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.formState(debtorAccountId = null), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.DEBTOR_LIST).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.PAYER_BANK_CHOICE).assertIsDisplayed()
    }

    /** Asking the bank to choose still leaves the payee unanswerable from saved payees. */
    @Test
    fun lettingTheBankChooseStillNeedsAManualPayee() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.formState(debtorAccountId = null, letBankChoosePayer = true),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.PAYEE_NEEDS_PAYER).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.MANUAL_ENTRY_BUTTON).assertIsDisplayed()
    }

    /** The amount is instructed in sterling, so a non-sterling payer is worth saying out loud. */
    @Test
    fun aNonSterlingPayerIsCalledOut() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.formState(debtorCurrency = "USD"), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.NON_GBP_NOTICE).assertIsDisplayed()
    }

    @Test
    fun aSterlingPayerGetsNoConversionNotice() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.formState(), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.NON_GBP_NOTICE).assertDoesNotExist()
    }

    /**
     * The two rails ask for different fields, because the bank accepts different fields.
     *
     * Reference is refused internationally with `U005`, and ChargeBearer is refused domestically —
     * so neither is a preference, and showing the wrong one would invite a request that cannot be
     * sent.
     */
    @Test
    fun theDomesticRailAsksForAReferenceAndNoCharges() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.formState(rail = PaymentRail.Domestic), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.REFERENCE_FIELD).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.CHARGE_BEARER_PICKER).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.CURRENCY_PICKER).assertDoesNotExist()
    }

    @Test
    fun theInternationalRailAsksForChargesAndCurrencyAndNoReference() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.formState(rail = PaymentRail.International),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.CURRENCY_PICKER).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.CHARGE_BEARER_PICKER).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REFERENCE_FIELD).assertDoesNotExist()
    }

    /** All four OBIE values are offered, even though only one is exercised by HSBC's samples. */
    @Test
    fun everyChargeBearerIsOffered() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.formState(rail = PaymentRail.International),
                {},
                {},
            )
        }
        ChargeBearer.entries.forEach { bearer ->
            onNodeWithTag(SendMoneyTestTags.chargeBearerChip(bearer)).performScrollTo().assertIsDisplayed()
        }
    }

    /** Only the two currencies Global Money accepts. */
    @Test
    fun onlyTheAcceptedTransferCurrenciesAreOffered() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.formState(rail = PaymentRail.International),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.currencyChip("USD")).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.currencyChip("EUR")).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.currencyChip("GBP")).assertDoesNotExist()
    }

    /** Each rail identifies a creditor its own way, and refuses the other's scheme with `U027`. */
    @Test
    fun manualEntryAsksForAnIbanOnTheInternationalRail() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.formState(rail = PaymentRail.International, manualEntryVisible = true),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.MANUAL_IBAN).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.MANUAL_SORT_CODE).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.MANUAL_ACCOUNT_NUMBER).assertDoesNotExist()
    }

    /**
     * The review describes the rail it is actually reviewing.
     *
     * "Sent via" read "Faster Payments" on both rails, which is untrue of an international payment,
     * and the two fields that rail turns on had no row at all — so a customer could approve a
     * currency conversion and a charge arrangement neither of which the review mentioned.
     */
    @Test
    fun theDomesticReviewNamesFasterPaymentsAndShowsTheReference() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.reviewState(rail = PaymentRail.Domestic), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.REVIEW_REFERENCE).performScrollTo().assertIsDisplayed()
        onNodeWithText("Faster Payments").assertExists()
        onNodeWithTag(SendMoneyTestTags.REVIEW_CURRENCY).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.REVIEW_CHARGE_BEARER).assertDoesNotExist()
    }

    @Test
    fun theInternationalReviewShowsTheCurrencyAndChargesAndNoReference() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(
                SendMoneyFixtures.reviewState(rail = PaymentRail.International),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.REVIEW_CURRENCY).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_CHARGE_BEARER).performScrollTo().assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_REFERENCE).assertDoesNotExist()
        onNodeWithText("Faster Payments").assertDoesNotExist()
    }

    /** With no payer of our own the row says so, rather than rendering as an empty field. */
    @Test
    fun theReviewSaysWhenTheBankWillChooseThePayer() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.reviewState(debtorAccountRow = null), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.REVIEW_FROM).performScrollTo().assertIsDisplayed()
        onNodeWithText("You’ll choose at your bank").assertExists()
    }

    /** Pinned, so it is reachable however far the form has been scrolled. */
    @Test
    fun theFormActionBarStaysOutsideTheScroll() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.filledFormState(), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.FORM_ACTIONS).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.REVIEW_BUTTON).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.FORM_TRUST_NOTE).assertIsDisplayed()
    }

    /** The rail cannot be changed once a specific payment is being reviewed. */
    @Test
    fun theReviewPageDropsTheRailToggleAndTheFormActions() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.reviewState(), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.REVIEW_PAGE).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.RAIL_TOGGLE).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.FORM_ACTIONS).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.FORM_PAGE).assertDoesNotExist()
    }

    /** TC-SEND-013: no payees is not a dead end. */
    @Test
    fun anEmptyPayeeListKeepsManualEntryAvailable() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.formState(beneficiaries = emptyList()), {}, {})
        }
        onNodeWithTag(SendMoneyTestTags.NO_SAVED_PAYEES).assertIsDisplayed()
        onNodeWithTag(SendMoneyTestTags.CREDITOR_LIST).assertDoesNotExist()
        onNodeWithTag(SendMoneyTestTags.MANUAL_ENTRY_BUTTON).assertIsDisplayed()
    }

    /** The manual fields sit below the payee list, so they need scrolling to before asserting. */
    @Test
    fun theManualFieldsAppearOnlyWhenAskedFor() = runComposeUiTest {
        setContent {
            SendMoneyScreenContent(SendMoneyFixtures.formState(manualEntryVisible = true), {}, {})
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
                SendMoneyFixtures.filledFormState(problem = SendMoneyAmountProblem.ExceedsAvailableBalance),
                {},
                {},
            )
        }
        onNodeWithTag(SendMoneyTestTags.AMOUNT_ERROR, useUnmergedTree = true).assertExists()
        onNodeWithTag(SendMoneyTestTags.REVIEW_BUTTON).assertIsNotEnabled()
    }

    /** TC-SMC-001: the amount leads, the detail rows confirm it, and both controls are present. */
    @Test
    fun theReviewPageLeadsWithTheAmountAndListsEveryDetail() = runComposeUiTest {
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
    fun theReviewPageShowsNoFeeFigureItCannotKnow() = runComposeUiTest {
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
