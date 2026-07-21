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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.feature.settings.ui.SettingsAction
import org.mifosx.openbanking.feature.settings.ui.SettingsErrorKind
import org.mifosx.openbanking.feature.settings.ui.SettingsState
import org.mifosx.openbanking.feature.settings.ui.SettingsUiState
import org.mifosx.openbanking.feature.settings.ui.labelResource
import kotlin.test.assertEquals

private const val ACCOUNT_ID = "40051512345678"
private const val APP_VERSION = "0.1.0 (build 1)"
private const val EXPECTED_ROW_COUNT = 6
private const val EXPECTED_SECTION_COUNT = 3

/**
 * androidInstrumentedTest does not see commonTest, so the state fixtures are inlined here.
 */
private fun contentState(
    themeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    isThemeMenuExpanded: Boolean = false,
): SettingsState = SettingsState(
    uiState = SettingsUiState.Content(
        themeConfig = themeConfig,
        themeLabel = themeConfig.labelResource(),
        appVersionLabel = APP_VERSION,
        isThemeMenuExpanded = isThemeMenuExpanded,
        selectedAccountId = ACCOUNT_ID,
    ),
)

private fun errorState(): SettingsState =
    SettingsState(uiState = SettingsUiState.Error(SettingsErrorKind.PreferencesUnavailable))

private fun emptyState(): SettingsState = SettingsState(uiState = SettingsUiState.Empty)

/**
 * On-device mirror of [SettingsScreenRobolectricTest], driving the same [SettingsTestTags] so a
 * divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<SettingsAction>()

    private fun render(state: SettingsState) {
        composeRule.setContent {
            SettingsScreenContent(state = state, onAction = { actions.add(it) })
        }
    }

    private fun countOf(tag: String): Int =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun contentRendersAllThreeSections() {
        render(contentState())

        composeRule.onNodeWithTag(SettingsTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.SECTION_APPEARANCE).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.SECTION_ACCOUNT).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.SECTION_ABOUT).assertExists()
    }

    @Test
    fun themeRowRendersItsValueAndDropdownChip() {
        render(contentState(themeConfig = DarkThemeConfig.DARK))

        composeRule.onNodeWithTag(SettingsTestTags.THEME_ROW).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.THEME_VALUE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.THEME_DROPDOWN, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun tappingTheThemeRowDispatchesToggleThemeMenu() {
        render(contentState())

        composeRule.onNodeWithTag(SettingsTestTags.THEME_ROW).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.ToggleThemeMenu), actions)
    }

    @Test
    fun selectingLightDispatchesSelectThemeWithLight() {
        render(contentState(isThemeMenuExpanded = true))

        composeRule.onNodeWithTag(SettingsTestTags.themeOption(DarkThemeConfig.LIGHT))
            .performClick()

        assertEquals(
            listOf<SettingsAction>(SettingsAction.SelectTheme(DarkThemeConfig.LIGHT)),
            actions,
        )
    }

    @Test
    fun accountAndAboutRowsCarryTheirTrailingIcons() {
        render(contentState())

        composeRule.onNodeWithTag(
            SettingsTestTags.chevron(SettingsTestTags.CONSENTS_ROW),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            SettingsTestTags.chevron(SettingsTestTags.PROFILE_ROW),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            SettingsTestTags.externalLink(SettingsTestTags.PRIVACY_ROW),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            SettingsTestTags.chevron(SettingsTestTags.LICENCES_ROW),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun appVersionIsStaticAndDispatchesNothingOnClick() {
        render(contentState())

        composeRule.onNodeWithTag(
            SettingsTestTags.chevron(SettingsTestTags.APP_VERSION_ROW),
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(SettingsTestTags.APP_VERSION_ROW).performClick()

        assertEquals(emptyList(), actions)
    }

    /** Pins the cut Security, Notifications and Clear Local Data surfaces on device too. */
    @Test
    fun neitherTheCutSectionsNorTheCutRowAreRendered() {
        render(contentState())

        assertEquals(EXPECTED_SECTION_COUNT, countOf(SettingsTestTags.SECTION))
        assertEquals(EXPECTED_ROW_COUNT, countOf(SettingsTestTags.ROW))
    }

    @Test
    fun errorStateRendersRetryAndDispatchesRetryLoad() {
        render(errorState())

        composeRule.onNodeWithTag(SettingsTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.RetryLoad), actions)
    }

    @Test
    fun emptyStateRendersTitleAndBodyWithNoSections() {
        render(emptyState())

        composeRule.onNodeWithTag(SettingsTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.EMPTY_TITLE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.EMPTY_BODY, useUnmergedTree = true)
            .assertExists()
        assertEquals(0, countOf(SettingsTestTags.SECTION))
    }
}
