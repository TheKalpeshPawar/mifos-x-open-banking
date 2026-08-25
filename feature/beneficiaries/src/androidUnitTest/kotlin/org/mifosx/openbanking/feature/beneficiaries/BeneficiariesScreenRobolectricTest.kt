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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesAction
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [BeneficiariesScreenContent] across its states under Robolectric (JVM, no device) and
 * drives it through the shared [BeneficiariesTestTags]. A verbatim on-device mirror lives in
 * [BeneficiariesScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class BeneficiariesScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<BeneficiariesAction>()

    private fun render(state: BeneficiariesState) {
        composeRule.setContent {
            BeneficiariesScreenContent(
                state = state,
                onAction = { actions.add(it) },
            )
        }
    }

    @Test
    fun contentRendersTheSearchFieldAndTheRows() {
        render(BeneficiariesFixtures.contentState())

        composeRule.onNodeWithTag(BeneficiariesTestTags.SEARCH_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(BeneficiariesTestTags.CONTENT_LIST).assertExists()
        composeRule.onNodeWithTag(BeneficiariesTestTags.row(BeneficiariesFixtures.FIRST_ID)).assertIsDisplayed()
    }

    @Test
    fun typingInTheSearchFieldRoutesTheQuery() {
        render(BeneficiariesFixtures.contentState())

        composeRule.onNodeWithTag(BeneficiariesTestTags.SEARCH_FIELD)
            .performTextInput(BeneficiariesFixtures.NAME_QUERY)

        assertEquals(
            listOf<BeneficiariesAction>(BeneficiariesAction.Search(BeneficiariesFixtures.NAME_QUERY)),
            actions,
        )
    }

    @Test
    fun tappingAPayeeRowRoutesNothing() {
        render(BeneficiariesFixtures.contentState())

        composeRule.onNodeWithTag(BeneficiariesTestTags.row(BeneficiariesFixtures.FIRST_ID)).performClick()

        assertTrue(actions.isEmpty())
    }
}
