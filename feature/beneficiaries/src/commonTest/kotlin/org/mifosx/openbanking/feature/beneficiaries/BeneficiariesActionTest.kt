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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesAction
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesErrorKind
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One case per `ui.yaml` action_contract: the search filter, the retry action, and the absence of a
 * row `on_click` — the 100% on_click coverage the behaviour gate requires.
 *
 * `navigate_back` is the screen's own `onBack` lambda rather than a routed action, so it is covered
 * by the Robolectric suite against the real scaffold instead.
 */
@OptIn(ExperimentalTestApi::class)
class BeneficiariesActionTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** filter_beneficiaries — typing in the search field dispatches Search with what was typed. */
    @Test
    fun typingInTheSearchFieldDispatchesSearch() = runComposeUiTest {
        val actions = mutableListOf<BeneficiariesAction>()
        setContent {
            BeneficiariesScreenContent(
                state = BeneficiariesFixtures.contentState(),
                onAction = { actions.add(it) },
            )
        }

        onNodeWithTag(BeneficiariesTestTags.SEARCH_FIELD).performTextInput(BeneficiariesFixtures.NAME_QUERY)

        assertEquals(
            listOf<BeneficiariesAction>(BeneficiariesAction.Search(BeneficiariesFixtures.NAME_QUERY)),
            actions,
        )
    }

    /** retry_load — the shared Retry button dispatches RetryLoad. */
    @Test
    fun theRetryButtonDispatchesRetryLoad() = runComposeUiTest {
        val actions = mutableListOf<BeneficiariesAction>()
        setContent {
            BeneficiariesScreenContent(
                state = BeneficiariesFixtures.errorState(BeneficiariesErrorKind.TokenExpired),
                onAction = { actions.add(it) },
            )
        }

        onNodeWithText("Retry").performClick()

        assertEquals(listOf<BeneficiariesAction>(BeneficiariesAction.RetryLoad), actions)
    }

    /**
     * No row on_click — a payee row is read-only.
     *
     * `ui.yaml` gives `beneficiary_row` no handler, and the misleading "tap to view payment details"
     * label was deliberately removed. Tapping a row must therefore route nothing at all; this pins
     * that so a later change cannot quietly make rows interactive.
     */
    @Test
    fun tappingAPayeeRowRoutesNothing() = runComposeUiTest {
        val actions = mutableListOf<BeneficiariesAction>()
        setContent {
            BeneficiariesScreenContent(
                state = BeneficiariesFixtures.contentState(),
                onAction = { actions.add(it) },
            )
        }

        onNodeWithTag(BeneficiariesTestTags.row(BeneficiariesFixtures.FIRST_ID)).assertIsDisplayed()
        onNodeWithTag(BeneficiariesTestTags.row(BeneficiariesFixtures.FIRST_ID)).performClick()

        assertTrue(actions.isEmpty())
    }
}
