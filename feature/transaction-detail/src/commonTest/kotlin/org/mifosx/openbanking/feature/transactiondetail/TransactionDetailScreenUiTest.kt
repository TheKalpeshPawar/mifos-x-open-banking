/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * Headless smoke coverage for [TransactionDetailScreenContent] on the desktop renderer, over the same
 * [TransactionDetailTestTags] the Robolectric and instrumented suites drive.
 *
 * This class lives in `commonTest`, so it also compiles into `androidUnitTest`, where there is no
 * Robolectric runner to stand up a composition. The module's build script filters it out of the
 * JVM unit-test tasks; the depth belongs to [TransactionDetailScreenRobolectricTest].
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native, whose frontend rejects
 * punctuation inside backticked names.
 */
@OptIn(ExperimentalTestApi::class)
class TransactionDetailScreenUiTest {

    @Test
    fun contentRendersTheHeroMerchantAndDetailCard() = runComposeUiTest {
        setContent {
            TransactionDetailScreenContent(
                state = TransactionDetailFixtures.contentState(),
                onAction = {},
                onBack = {},
            )
        }

        onNodeWithTag(TransactionDetailTestTags.CONTENT_ROOT).assertExists()
        onNodeWithTag(TransactionDetailTestTags.AMOUNT).assertExists()
        onNodeWithTag(TransactionDetailTestTags.MERCHANT).assertExists()
        onNodeWithTag(TransactionDetailTestTags.DETAIL_CARD).assertExists()
        onNodeWithTag(TransactionDetailTestTags.REFERENCE_ROW).assertExists()
        onNodeWithTag(TransactionDetailTestTags.MCC_ROW).assertExists()
    }

    @Test
    fun contentWithoutAnMccHidesTheMccRow() = runComposeUiTest {
        setContent {
            TransactionDetailScreenContent(
                state = TransactionDetailFixtures.mccNullContentState(),
                onAction = {},
                onBack = {},
            )
        }

        onNodeWithTag(TransactionDetailTestTags.MCC_ROW).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSpinnerNotTheContent() = runComposeUiTest {
        setContent {
            TransactionDetailScreenContent(
                state = TransactionDetailFixtures.loadingState(),
                onAction = {},
                onBack = {},
            )
        }

        onNodeWithTag(TransactionDetailTestTags.LOADING_INDICATOR).assertExists()
        onNodeWithTag(TransactionDetailTestTags.CONTENT_ROOT).assertDoesNotExist()
    }

    @Test
    fun aRecoverableErrorShowsRetryNotGoBack() = runComposeUiTest {
        setContent {
            TransactionDetailScreenContent(
                state = TransactionDetailFixtures.recoverableErrorState(),
                onAction = {},
                onBack = {},
            )
        }

        onNodeWithTag(TransactionDetailTestTags.ERROR_STATE).assertExists()
        onNodeWithTag(TransactionDetailTestTags.RETRY_BUTTON).assertExists()
        onNodeWithTag(TransactionDetailTestTags.GO_BACK_BUTTON).assertDoesNotExist()
    }

    @Test
    fun aNonRecoverableErrorShowsGoBackNotRetry() = runComposeUiTest {
        setContent {
            TransactionDetailScreenContent(
                state = TransactionDetailFixtures.nonRecoverableErrorState(),
                onAction = {},
                onBack = {},
            )
        }

        onNodeWithTag(TransactionDetailTestTags.ERROR_STATE).assertExists()
        onNodeWithTag(TransactionDetailTestTags.GO_BACK_BUTTON).assertExists()
        onNodeWithTag(TransactionDetailTestTags.RETRY_BUTTON).assertDoesNotExist()
    }

    @Test
    fun emptyRendersItsTitleAndBackButton() = runComposeUiTest {
        setContent {
            TransactionDetailScreenContent(
                state = TransactionDetailFixtures.emptyState(),
                onAction = {},
                onBack = {},
            )
        }

        onNodeWithTag(TransactionDetailTestTags.EMPTY_STATE).assertExists()
        onNodeWithTag(TransactionDetailTestTags.EMPTY_TITLE, useUnmergedTree = true).assertExists()
        onNodeWithTag(TransactionDetailTestTags.EMPTY_BACK_BUTTON).assertExists()
    }
}
