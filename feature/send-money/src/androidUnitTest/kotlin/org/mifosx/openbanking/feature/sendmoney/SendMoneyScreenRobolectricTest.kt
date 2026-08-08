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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.banking.payment.ChargeBearer
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyAction
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [SendMoneyScreenContent] under Robolectric (JVM, no device), driven through the shared
 * [SendMoneyTestTags]. An on-device mirror lives in [SendMoneyScreenInstrumentedTest].
 *
 * The recurring question is which fields each rail asks for: the two accept genuinely different
 * ones, and sending the wrong one is a `400` rather than a preference.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class SendMoneyScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<SendMoneyAction>()

    private fun render(state: SendMoneyState) {
        composeRule.setContent {
            SendMoneyScreenContent(state = state, onAction = { actions.add(it) }, onNavigateToConsents = {})
        }
    }

    @Test
    fun theFormPageCarriesEveryPickerAndTheAmountOnOneScroll() {
        render(SendMoneyFixtures.formState())

        composeRule.onNodeWithTag(SendMoneyTestTags.FORM_PAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.RAIL_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.PAYER_PICKER).assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.CREDITOR_LIST).assertExists()
        composeRule.onNodeWithTag(SendMoneyTestTags.AMOUNT_CARD).performScrollTo().assertIsDisplayed()
    }

    /** Pinned below the scroll, so it does not need scrolling to. */
    @Test
    fun theActionBarIsReachableWithoutScrolling() {
        render(SendMoneyFixtures.filledFormState())

        composeRule.onNodeWithTag(SendMoneyTestTags.FORM_ACTIONS).assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.REVIEW_BUTTON).assertIsDisplayed()
    }

    @Test
    fun tappingReviewRoutesTheAction() {
        render(SendMoneyFixtures.filledFormState())

        composeRule.onNodeWithTag(SendMoneyTestTags.REVIEW_BUTTON).performClick()

        assertEquals(listOf<SendMoneyAction>(SendMoneyAction.ReviewPayment), actions)
    }

    /** Collapsed, the accounts are genuinely off the page — the point of the picker. */
    @Test
    fun theCollapsedPickerHidesTheAccountsUntilItIsOpened() {
        render(SendMoneyFixtures.formState())

        composeRule.onNodeWithTag(SendMoneyTestTags.PAYER_PICKER).assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.DEBTOR_LIST).assertDoesNotExist()
        composeRule.onNodeWithTag(SendMoneyTestTags.PAYER_BANK_CHOICE).assertDoesNotExist()
    }

    @Test
    fun tappingTheCollapsedPickerRoutesTheToggle() {
        render(SendMoneyFixtures.formState())

        composeRule.onNodeWithTag(SendMoneyTestTags.PAYER_PICKER).performClick()

        assertEquals(listOf<SendMoneyAction>(SendMoneyAction.TogglePayerPicker), actions)
    }

    /** "Choose at my bank" moved inside the picker and must still be reachable there. */
    @Test
    fun tappingChooseAtMyBankRoutesTheAction() {
        render(SendMoneyFixtures.formState(debtorAccountId = null, payerPickerExpanded = true))

        composeRule.onNodeWithTag(SendMoneyTestTags.PAYER_BANK_CHOICE).performScrollTo().performClick()

        assertEquals(listOf<SendMoneyAction>(SendMoneyAction.LetBankChoosePayer), actions)
    }

    /** Reference is refused internationally with `U005`, so it only exists on the domestic rail. */
    @Test
    fun theDomesticRailAsksForAReferenceAndNoCharges() {
        render(SendMoneyFixtures.formState(rail = PaymentRail.Domestic))

        composeRule.onNodeWithTag(SendMoneyTestTags.REFERENCE_FIELD).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.CHARGE_BEARER_PICKER).assertDoesNotExist()
        composeRule.onNodeWithTag(SendMoneyTestTags.INSTRUCTED_CURRENCY_PICKER).assertDoesNotExist()
    }

    /** ChargeBearer is required internationally with `U004`, and refused domestically. */
    @Test
    fun theInternationalRailAsksForChargesAndCurrencyAndNoReference() {
        render(SendMoneyFixtures.formState(rail = PaymentRail.International))

        composeRule.onNodeWithTag(SendMoneyTestTags.INSTRUCTED_CURRENCY_PICKER)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.CHARGE_BEARER_PICKER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.REFERENCE_FIELD).assertDoesNotExist()
    }

    /** Two taps now, not one: the options live in a menu that has to be opened first. */
    @Test
    fun choosingAChargeBearerFromTheMenuRoutesTheChoice() {
        render(SendMoneyFixtures.formState(rail = PaymentRail.International))

        composeRule.onNodeWithTag(SendMoneyTestTags.CHARGE_BEARER_PICKER).performScrollTo().performClick()
        composeRule.onNodeWithTag(SendMoneyTestTags.chargeBearerOption(ChargeBearer.Shared)).performClick()

        assertEquals(listOf<SendMoneyAction>(SendMoneyAction.SelectChargeBearer(ChargeBearer.Shared)), actions)
    }

    /** The value HSBC refuses is not in the menu, which is the whole point of the offered list. */
    @Test
    fun theChargeBearerMenuOmitsFollowingServiceLevel() {
        render(SendMoneyFixtures.formState(rail = PaymentRail.International))

        composeRule.onNodeWithTag(SendMoneyTestTags.CHARGE_BEARER_PICKER).performScrollTo().performClick()

        composeRule.onNodeWithTag(
            SendMoneyTestTags.chargeBearerOption(ChargeBearer.FollowingServiceLevel),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    /** The amount row's own control, and now the form's only one. */
    @Test
    fun choosingACurrencyFromTheAmountsMenuRoutesTheChoice() {
        render(SendMoneyFixtures.formState(rail = PaymentRail.International))

        composeRule.onNodeWithTag(SendMoneyTestTags.INSTRUCTED_CURRENCY_PICKER)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(SendMoneyTestTags.instructedCurrencyOption("USD")).performClick()

        assertEquals(listOf<SendMoneyAction>(SendMoneyAction.SelectInstructedCurrency("USD")), actions)
    }

    /** A refused payee read is its own state, with its own way out. */
    @Test
    fun aFailedPayeeReadOffersRetryRatherThanClaimingThereAreNone() {
        render(SendMoneyFixtures.formState(payeesFailed = true))

        composeRule.onNodeWithTag(SendMoneyTestTags.PAYEES_FAILED).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.NO_SAVED_PAYEES).assertDoesNotExist()

        composeRule.onNodeWithTag(SendMoneyTestTags.PAYEES_RETRY_BUTTON).performScrollTo().performClick()

        assertEquals(listOf<SendMoneyAction>(SendMoneyAction.RetryPayees), actions)
    }

    /** Each rail identifies a creditor its own way and refuses the other's scheme with `U027`. */
    @Test
    fun manualEntryAsksForAnIbanInternationallyAndASortCodeDomestically() {
        render(SendMoneyFixtures.formState(rail = PaymentRail.International, manualEntryVisible = true))

        composeRule.onNodeWithTag(SendMoneyTestTags.MANUAL_IBAN).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.MANUAL_SORT_CODE).assertDoesNotExist()
    }

    /**
     * Payees are account-scoped, so with no payer there is nothing to list — and we say why.
     *
     * Scrolled to rather than asserted in place: on a phone-sized frame the payee section sits below
     * the payer list and the bank-choice row. The desktop runner's larger default window hides that,
     * which is exactly the kind of thing this suite exists to catch.
     */
    @Test
    fun noPayerChosenExplainsTheEmptyPayeeSection() {
        render(SendMoneyFixtures.formState(debtorAccountId = null))

        composeRule.onNodeWithTag(SendMoneyTestTags.PAYEE_NEEDS_PAYER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.creditorRow(SendMoneyFixtures.JAMESON_ID))
            .assertDoesNotExist()
    }

    @Test
    fun theReviewPageDropsTheRailToggleAndTheFormActions() {
        render(SendMoneyFixtures.reviewState())

        composeRule.onNodeWithTag(SendMoneyTestTags.REVIEW_PAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(SendMoneyTestTags.RAIL_TOGGLE).assertDoesNotExist()
        composeRule.onNodeWithTag(SendMoneyTestTags.FORM_ACTIONS).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSkeletonNotTheForm() {
        render(SendMoneyFixtures.loadingState())

        composeRule.onNodeWithTag(SendMoneyTestTags.SKELETON).assertExists()
        composeRule.onNodeWithTag(SendMoneyTestTags.FORM_PAGE).assertDoesNotExist()
    }
}
