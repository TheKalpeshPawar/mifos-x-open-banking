/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * Headless smoke pass over [AccountHolderScreenContent], covering the content state rather than the
 * detail of each surface — that is what the Robolectric and instrumented suites are for.
 *
 * Lives in commonTest so it compiles for every target, and is excluded from the Android unit-test
 * tasks by the module's build script: there is no Robolectric runner there to supply an Android
 * runtime, so every case would NPE.
 */
@OptIn(ExperimentalTestApi::class)
class AccountHolderScreenUiTest {

    @Test
    fun contentRendersTheIdentity() = runComposeUiTest {
        setContent { AccountHolderScreenContent(state = AccountHolderFixtures.contentState(), onAction = {}) }

        onNodeWithTag(AccountHolderTestTags.IDENTITY_CARD).assertExists()
        onNodeWithTag(AccountHolderTestTags.IDENTITY_SECTION).assertExists()
    }
}
