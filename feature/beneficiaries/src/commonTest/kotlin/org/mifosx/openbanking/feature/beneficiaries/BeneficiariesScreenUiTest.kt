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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * Headless smoke coverage for [BeneficiariesScreenContent] on the desktop renderer, over the same
 * [BeneficiariesTestTags] the Robolectric and instrumented suites drive.
 *
 * This class lives in `commonTest`, so it also compiles into `androidUnitTest`, where there is no
 * Robolectric runner; the module's build script filters it out of the JVM unit-test tasks.
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native.
 */
@OptIn(ExperimentalTestApi::class)
class BeneficiariesScreenUiTest {

    @Test
    fun contentRendersTheSearchFieldAndEveryPayeeRow() = runComposeUiTest {
        setContent {
            BeneficiariesScreenContent(
                state = BeneficiariesFixtures.contentState(),
                onAction = {},
            )
        }

        onNodeWithTag(BeneficiariesTestTags.SEARCH_FIELD).assertExists()
        onNodeWithTag(BeneficiariesTestTags.CONTENT_LIST).assertExists()
        onNodeWithTag(BeneficiariesTestTags.row(BeneficiariesFixtures.FIRST_ID)).assertExists()
        onNodeWithTag(BeneficiariesTestTags.row(BeneficiariesFixtures.ENERGY_ID)).assertExists()
    }

    @Test
    fun aSearchWithMatchesRendersOnlyTheMatchingRow() = runComposeUiTest {
        setContent {
            BeneficiariesScreenContent(
                state = BeneficiariesFixtures.searchedState(),
                onAction = {},
            )
        }

        onNodeWithTag(BeneficiariesTestTags.row(BeneficiariesFixtures.ENERGY_ID)).assertExists()
        onNodeWithTag(BeneficiariesTestTags.row(BeneficiariesFixtures.FIRST_ID)).assertDoesNotExist()
    }
}
