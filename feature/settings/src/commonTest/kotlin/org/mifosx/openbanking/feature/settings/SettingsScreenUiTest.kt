/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.settings.ui.SettingsAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Headless smoke coverage for [SettingsScreenContent] on the desktop renderer, over the same
 * [SettingsTestTags] the Robolectric and instrumented suites drive.
 *
 * This class lives in `commonTest`, so it also compiles into `androidUnitTest`, where there is no
 * Robolectric runner to stand up a composition. The module's build script filters it out of the
 * JVM unit-test tasks; the depth belongs to [SettingsScreenRobolectricTest].
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native, whose frontend rejects
 * punctuation inside backticked names.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsScreenUiTest {

    @Test
    fun contentRendersAllThreeSections() = runComposeUiTest {
        setContent {
            SettingsScreenContent(state = SettingsFixtures.contentState(), onAction = {})
        }

        onNodeWithTag(SettingsTestTags.CONTENT).assertExists()
        onNodeWithTag(SettingsTestTags.SECTION_APPEARANCE).assertExists()
        onNodeWithTag(SettingsTestTags.SECTION_ACCOUNT).assertExists()
        onNodeWithTag(SettingsTestTags.SECTION_ABOUT).assertExists()
        assertEquals(
            SettingsFixtures.EXPECTED_SECTION_COUNT,
            onAllNodesWithTag(SettingsTestTags.SECTION, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )
    }

    @Test
    fun contentRendersEveryRowAndNoOthers() = runComposeUiTest {
        setContent {
            SettingsScreenContent(state = SettingsFixtures.contentState(), onAction = {})
        }

        assertEquals(
            SettingsFixtures.EXPECTED_ROW_COUNT,
            onAllNodesWithTag(SettingsTestTags.ROW, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )
    }

    @Test
    fun tappingTheThemeRowDispatchesToggleThemeMenu() = runComposeUiTest {
        val actions = mutableListOf<SettingsAction>()
        setContent {
            SettingsScreenContent(
                state = SettingsFixtures.contentState(),
                onAction = { actions.add(it) },
            )
        }

        onNodeWithTag(SettingsTestTags.THEME_ROW).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.ToggleThemeMenu), actions)
    }

    @Test
    fun tappingLicencesRowFiresNavigateToLicencesAndKeepsItsChevron() = runComposeUiTest {
        var navigated = false
        setContent {
            SettingsScreenContent(
                state = SettingsFixtures.contentState(),
                onAction = {},
                onNavigateToLicences = { navigated = true },
            )
        }

        onNodeWithTag(SettingsTestTags.LICENCES_ROW).performClick()

        assertTrue(navigated)
        onNodeWithTag(
            SettingsTestTags.chevron(SettingsTestTags.LICENCES_ROW),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun tappingPrivacyRowOpensTheMifosPrivacyPolicyUrl() = runComposeUiTest {
        val opened = mutableListOf<String>()
        setContent {
            SettingsScreenContent(
                state = SettingsFixtures.contentState(),
                onAction = {},
                onOpenUrl = { opened.add(it) },
            )
        }

        onNodeWithTag(SettingsTestTags.PRIVACY_ROW).performClick()

        assertEquals(listOf("https://mifos.org/privacy-policy/"), opened)
    }

    @Test
    fun errorRendersRetryAndDispatchesRetryLoad() = runComposeUiTest {
        val actions = mutableListOf<SettingsAction>()
        setContent {
            SettingsScreenContent(
                state = SettingsFixtures.errorState(),
                onAction = { actions.add(it) },
            )
        }

        onNodeWithTag(SettingsTestTags.ERROR_STATE).assertExists()
        onNodeWithTag(SettingsTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.RetryLoad), actions)
    }

    @Test
    fun emptyRendersItsTitleAndBody() = runComposeUiTest {
        setContent {
            SettingsScreenContent(state = SettingsFixtures.emptyState(), onAction = {})
        }

        onNodeWithTag(SettingsTestTags.EMPTY_STATE).assertExists()
        onNodeWithTag(SettingsTestTags.EMPTY_TITLE, useUnmergedTree = true).assertExists()
        onNodeWithTag(SettingsTestTags.EMPTY_BODY, useUnmergedTree = true).assertExists()
    }
}
