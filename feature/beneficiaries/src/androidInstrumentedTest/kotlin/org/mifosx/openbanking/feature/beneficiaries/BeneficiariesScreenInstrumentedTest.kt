/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesAction
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesErrorKind
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesState
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesUiState
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiaryRowUi
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ACCOUNT_ID = "40051512345678"
private const val FIRST_ID = "BEN-001"
private const val ENERGY_ID = "BEN-003"
private const val NAME_QUERY = "ener"
private const val NO_MATCH_QUERY = "zzzmatch"

/**
 * androidInstrumentedTest does not see commonTest, so the state fixtures are inlined here.
 */
private fun rows(): List<BeneficiaryRowUi> = listOf(
    BeneficiaryRowUi(
        beneficiaryId = FIRST_ID,
        name = "Jameson Lettings",
        scheme = BeneficiaryScheme.SortCode,
        identification = "40-12-09 65872310",
        reference = "RENT-FLAT12",
    ),
    BeneficiaryRowUi(
        beneficiaryId = ENERGY_ID,
        name = "EDF Energy",
        scheme = BeneficiaryScheme.SortCode,
        identification = "60-00-01 99887766",
        reference = "ELEC-8841",
    ),
)

private fun contentState(): BeneficiariesState = BeneficiariesState(
    accountId = ACCOUNT_ID,
    uiState = BeneficiariesUiState.Content(all = rows(), filtered = rows(), query = ""),
)

private fun searchWithoutMatchesState(): BeneficiariesState = BeneficiariesState(
    accountId = ACCOUNT_ID,
    uiState = BeneficiariesUiState.Content(all = rows(), filtered = emptyList(), query = NO_MATCH_QUERY),
)

private fun loadingState(): BeneficiariesState =
    BeneficiariesState(accountId = ACCOUNT_ID, uiState = BeneficiariesUiState.Loading)

private fun emptyState(): BeneficiariesState =
    BeneficiariesState(accountId = ACCOUNT_ID, uiState = BeneficiariesUiState.Empty)

private fun errorState(kind: BeneficiariesErrorKind): BeneficiariesState =
    BeneficiariesState(accountId = ACCOUNT_ID, uiState = BeneficiariesUiState.Error(kind))

/**
 * On-device mirror of [BeneficiariesScreenRobolectricTest], driving the same
 * [BeneficiariesTestTags] so a divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class BeneficiariesScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<BeneficiariesAction>()
    private var navigatedToConsents = false

    private fun render(state: BeneficiariesState) {
        composeRule.setContent {
            BeneficiariesScreenContent(
                state = state,
                onAction = { actions.add(it) },
                onNavigateToConsents = { navigatedToConsents = true },
            )
        }
    }

    @Test
    fun contentRendersTheSearchFieldAndTheRows() {
        render(contentState())

        composeRule.onNodeWithTag(BeneficiariesTestTags.SEARCH_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(BeneficiariesTestTags.CONTENT_LIST).assertExists()
        composeRule.onNodeWithTag(BeneficiariesTestTags.row(FIRST_ID)).assertIsDisplayed()
    }

    @Test
    fun typingInTheSearchFieldRoutesTheQuery() {
        render(contentState())

        composeRule.onNodeWithTag(BeneficiariesTestTags.SEARCH_FIELD).performTextInput(NAME_QUERY)

        assertEquals(listOf<BeneficiariesAction>(BeneficiariesAction.Search(NAME_QUERY)), actions)
    }

    @Test
    fun tappingAPayeeRowRoutesNothing() {
        render(contentState())

        composeRule.onNodeWithTag(BeneficiariesTestTags.row(FIRST_ID)).performClick()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun loadingRendersTheSpinnerNotTheList() {
        render(loadingState())

        composeRule.onNodeWithTag(BeneficiariesTestTags.LOADING).assertExists()
        composeRule.onNodeWithTag(BeneficiariesTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyHidesTheSearchFieldAndTheList() {
        render(emptyState())

        composeRule.onNodeWithTag(BeneficiariesTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(BeneficiariesTestTags.SEARCH_FIELD).assertDoesNotExist()
    }

    @Test
    fun aSearchWithoutMatchesKeepsTheFieldAndSwapsInTheNoResultsBlock() {
        render(searchWithoutMatchesState())

        composeRule.onNodeWithTag(BeneficiariesTestTags.SEARCH_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(BeneficiariesTestTags.SEARCH_EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(BeneficiariesTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun aRetriableErrorShowsRetryAndDispatchesRetryLoad() {
        render(errorState(BeneficiariesErrorKind.RateLimited))

        composeRule.onNodeWithTag(BeneficiariesTestTags.VIEW_CONSENTS_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(BeneficiariesTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<BeneficiariesAction>(BeneficiariesAction.RetryLoad), actions)
    }

    @Test
    fun aRevokedConsentShowsViewConsentsAndRaisesTheHostRoute() {
        render(errorState(BeneficiariesErrorKind.ConsentRevoked))

        composeRule.onNodeWithTag(BeneficiariesTestTags.RETRY_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(BeneficiariesTestTags.VIEW_CONSENTS_BUTTON).performClick()

        assertTrue(navigatedToConsents)
    }
}
