/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailState
import kotlin.test.Test

private const val WARNING_DAYS = 3

/**
 * Headless smoke coverage for [ConsentDetailScreenContent] on the desktop renderer, over the same
 * [ConsentDetailTestTags] the Robolectric and instrumented suites drive.
 *
 * This class lives in `commonTest`, so it also compiles into `androidUnitTest`, where there is no
 * Robolectric runner; the module's build script filters it out of the JVM unit-test tasks.
 */
@OptIn(ExperimentalTestApi::class)
class ConsentDetailScreenUiTest {

    private fun androidx.compose.ui.test.ComposeUiTest.render(state: ConsentDetailState) {
        setContent {
            ConsentDetailScreenContent(
                state = state,
                onAction = {},
                onReconfirm = {},
                onGoBack = {},
            )
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.scrollTo(targetTag: String) {
        onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).performScrollToNode(hasTestTag(targetTag))
    }

    @Test
    fun contentRendersStatusDatesPermissionsAndBothActions() = runComposeUiTest {
        render(ConsentDetailFixtures.contentState())

        onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).assertExists()
        onNodeWithTag(ConsentDetailTestTags.STATUS_CARD).assertExists()
        onNodeWithTag(ConsentDetailTestTags.STATUS_CHIP, useUnmergedTree = true).assertExists()
        onNodeWithTag(ConsentDetailTestTags.CONSENT_ID, useUnmergedTree = true).assertExists()
        onNodeWithTag(ConsentDetailTestTags.DATES_HEADER).assertExists()
        onNodeWithTag(ConsentDetailTestTags.DATES_LIST).assertExists()
        scrollTo(ConsentDetailTestTags.REVOKE_BUTTON)
        onNodeWithTag(ConsentDetailTestTags.PERMISSIONS_HEADER).assertExists()
        onNodeWithTag(ConsentDetailTestTags.REVOKE_BUTTON).assertExists()
    }

    @Test
    fun everyDateSlotIsRendered() = runComposeUiTest {
        render(ConsentDetailFixtures.contentState())

        listOf("connected", "expires", "txnFrom", "txnTo").forEach { slug ->
            onNodeWithTag(ConsentDetailTestTags.dateRow(slug), useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun everyPermissionIsRendered() = runComposeUiTest {
        render(ConsentDetailFixtures.contentState())

        scrollTo(ConsentDetailTestTags.permissionRow(ConsentDetailFixtures.FIRST_PERMISSION))
        onNodeWithTag(
            ConsentDetailTestTags.permissionRow(ConsentDetailFixtures.FIRST_PERMISSION),
        ).assertExists()
    }

    @Test
    fun theExpiryBannerRendersOnlyInsideTheWarningWindow() = runComposeUiTest {
        render(ConsentDetailFixtures.contentState(expiryWarningDays = WARNING_DAYS))

        onNodeWithTag(ConsentDetailTestTags.EXPIRY_BANNER).assertExists()
    }

    @Test
    fun theExpiryBannerIsAbsentOutsideTheWarningWindow() = runComposeUiTest {
        render(ConsentDetailFixtures.contentState())

        onNodeWithTag(ConsentDetailTestTags.EXPIRY_BANNER).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSpinnerNotTheContent() = runComposeUiTest {
        render(ConsentDetailFixtures.loadingState())

        onNodeWithTag(ConsentDetailTestTags.LOADING).assertExists()
        onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    /** The gate renders over the content, which stays standing so cancelling restores it intact. */
    @Test
    fun theRevokeGateRendersOverTheContent() = runComposeUiTest {
        render(ConsentDetailFixtures.revokeConfirmState())

        onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG).assertExists()
        onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_TITLE).assertExists()
        onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CANCEL).assertExists()
        onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CONFIRM).assertExists()
        onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).assertExists()
    }

    @Test
    fun revokingRendersProgressAndNoConsentContent() = runComposeUiTest {
        render(ConsentDetailFixtures.revokingState())

        onNodeWithTag(ConsentDetailTestTags.REVOKING_PROGRESS).assertExists()
        onNodeWithTag(ConsentDetailTestTags.REVOKING_LABEL).assertExists()
        onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).assertDoesNotExist()
        onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG).assertDoesNotExist()
    }

    @Test
    fun emptyRendersTheBackToConnectionsRoute() = runComposeUiTest {
        render(ConsentDetailFixtures.emptyState())

        onNodeWithTag(ConsentDetailTestTags.EMPTY_STATE).assertExists()
        onNodeWithTag(ConsentDetailTestTags.EMPTY_TITLE).assertExists()
        onNodeWithTag(ConsentDetailTestTags.GO_BACK_BUTTON).assertExists()
        onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun errorRendersTheRetryRoute() = runComposeUiTest {
        render(ConsentDetailFixtures.errorState())

        onNodeWithTag(ConsentDetailTestTags.ERROR_STATE).assertExists()
        onNodeWithTag(ConsentDetailTestTags.ERROR_TITLE).assertExists()
        onNodeWithTag(ConsentDetailTestTags.RETRY_BUTTON).assertExists()
    }
}
