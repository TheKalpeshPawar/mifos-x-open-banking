/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.scheduledpayments

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * Headless smoke coverage for [ScheduledPaymentsScreenContent] on the desktop renderer, over the
 * same [ScheduledPaymentsTestTags] the Robolectric and instrumented suites drive.
 *
 * This class lives in `commonTest`, so it also compiles into `androidUnitTest`, where there is no
 * Robolectric runner to stand up a composition. The module's build script filters it out of the
 * JVM unit-test tasks; the depth belongs to [ScheduledPaymentsScreenRobolectricTest].
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native, whose frontend rejects
 * punctuation inside backticked names.
 */
@OptIn(ExperimentalTestApi::class)
class ScheduledPaymentsScreenUiTest {

    @Test
    fun contentRendersTheListAndBothTypeChips() = runComposeUiTest {
        setContent {
            ScheduledPaymentsScreenContent(
                state = ScheduledPaymentsFixtures.contentState(),
                onAction = {},
            )
        }

        onNodeWithTag(ScheduledPaymentsTestTags.CONTENT_LIST).assertExists()
        onNodeWithTag(ScheduledPaymentsTestTags.card(ScheduledPaymentsFixtures.EXECUTION_ID)).assertExists()
        onNodeWithTag(ScheduledPaymentsTestTags.card(ScheduledPaymentsFixtures.ARRIVAL_ID)).assertExists()
    }
}
