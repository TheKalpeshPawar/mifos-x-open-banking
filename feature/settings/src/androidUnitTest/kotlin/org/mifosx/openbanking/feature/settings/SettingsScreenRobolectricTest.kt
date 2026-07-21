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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.feature.settings.ui.SettingsAction
import org.mifosx.openbanking.feature.settings.ui.SettingsState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [SettingsScreenContent] across its states under Robolectric (JVM, no device) and drives
 * it through the shared [SettingsTestTags]. A verbatim on-device mirror lives in
 * [SettingsScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class SettingsScreenRobolectricTest {

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

    private fun topOf(tag: String): Float = composeRule
        .onNodeWithTag(tag, useUnmergedTree = true)
        .fetchSemanticsNode()
        .positionInRoot
        .y

    @Test
    fun allThreeSectionsRenderInThePreviewOrder() {
        render(SettingsFixtures.contentState())

        composeRule.onNodeWithTag(SettingsTestTags.CONTENT).assertExists()
        assertTrue(topOf(SettingsTestTags.SECTION_APPEARANCE) < topOf(SettingsTestTags.SECTION_ACCOUNT))
        assertTrue(topOf(SettingsTestTags.SECTION_ACCOUNT) < topOf(SettingsTestTags.SECTION_ABOUT))
    }

    @Test
    fun themeRowRendersTheCurrentValueAndItsDropdownChip() {
        render(SettingsFixtures.contentState(themeConfig = DarkThemeConfig.DARK))

        composeRule.onNodeWithTag(SettingsTestTags.THEME_ROW).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.THEME_VALUE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.THEME_DROPDOWN, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun tappingTheThemeRowDispatchesToggleThemeMenu() {
        render(SettingsFixtures.contentState())

        composeRule.onNodeWithTag(SettingsTestTags.THEME_ROW).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.ToggleThemeMenu), actions)
    }

    @Test
    fun anExpandedMenuOffersEveryThemeOption() {
        render(SettingsFixtures.contentState(isThemeMenuExpanded = true))

        DarkThemeConfig.entries.forEach { config ->
            composeRule.onNodeWithTag(SettingsTestTags.themeOption(config), useUnmergedTree = true)
                .assertExists()
        }
    }

    @Test
    fun selectingLightDispatchesSelectThemeWithLight() {
        render(SettingsFixtures.contentState(isThemeMenuExpanded = true))

        composeRule.onNodeWithTag(SettingsTestTags.themeOption(DarkThemeConfig.LIGHT))
            .performClick()

        assertEquals(
            listOf<SettingsAction>(SettingsAction.SelectTheme(DarkThemeConfig.LIGHT)),
            actions,
        )
    }

    @Test
    fun accountSectionRendersConsentsAndProfileEachWithAChevron() {
        render(SettingsFixtures.contentState())

        composeRule.onNodeWithTag(SettingsTestTags.CONSENTS_ROW).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.PROFILE_ROW).assertExists()
        composeRule.onNodeWithTag(
            SettingsTestTags.chevron(SettingsTestTags.CONSENTS_ROW),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            SettingsTestTags.chevron(SettingsTestTags.PROFILE_ROW),
            useUnmergedTree = true,
        ).assertExists()
    }

    /** Privacy leaves the app; Licences stays inside it. The glyphs must say so. */
    @Test
    fun aboutRowsCarryTheTrailingIconTheirDestinationImplies() {
        render(SettingsFixtures.contentState())

        composeRule.onNodeWithTag(
            SettingsTestTags.externalLink(SettingsTestTags.PRIVACY_ROW),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            SettingsTestTags.chevron(SettingsTestTags.LICENCES_ROW),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            SettingsTestTags.externalLink(SettingsTestTags.LICENCES_ROW),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun appVersionRowIsStaticWithNoTrailingAffordance() {
        render(SettingsFixtures.contentState())

        composeRule.onNodeWithTag(SettingsTestTags.APP_VERSION_ROW).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.APP_VERSION_VALUE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(
            SettingsTestTags.chevron(SettingsTestTags.APP_VERSION_ROW),
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(
            SettingsTestTags.externalLink(SettingsTestTags.APP_VERSION_ROW),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun tappingAppVersionDispatchesNothing() {
        render(SettingsFixtures.contentState())

        composeRule.onNodeWithTag(SettingsTestTags.APP_VERSION_ROW).performClick()

        assertEquals(emptyList(), actions)
    }

    /**
     * The rendered preview still carries Security and Notifications groups that this screen
     * deliberately does not offer. Counting sections pins the exclusion against a design that
     * disagrees with it.
     */
    @Test
    fun noSecurityOrNotificationsSectionIsRendered() {
        render(SettingsFixtures.contentState())

        assertEquals(SettingsFixtures.EXPECTED_SECTION_COUNT, countOf(SettingsTestTags.SECTION))
        composeRule.onNodeWithTag(SettingsTestTags.SECTION_APPEARANCE).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.SECTION_ACCOUNT).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.SECTION_ABOUT).assertExists()
    }

    /**
     * The preview's Account group has a third, destructive Clear Local Data row. Counting rows
     * fails the moment it — or any other row — is added back.
     */
    @Test
    fun noClearLocalDataRowIsRendered() {
        render(SettingsFixtures.contentState())

        assertEquals(SettingsFixtures.EXPECTED_ROW_COUNT, countOf(SettingsTestTags.ROW))
    }

    @Test
    fun errorStateRendersRetryAndDispatchesRetryLoad() {
        render(SettingsFixtures.errorState())

        composeRule.onNodeWithTag(SettingsTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.ERROR_TITLE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.ERROR_BODY, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag(SettingsTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.RetryLoad), actions)
    }

    /**
     * The licences screen composes without a crash and stands up its scaffold. The AboutLibraries
     * catalogue loads asynchronously off a Compose resource, so this pins the frame around it rather
     * than the list contents, which are covered on device.
     */
    @Test
    fun licencesScreenRendersItsScaffoldFrame() {
        composeRule.setContent { LicencesScreen(onBack = {}) }

        composeRule.onNodeWithTag(SettingsTestTags.LICENCES_SCREEN).assertExists()
    }

    @Test
    fun emptyStateRendersTitleAndBodyWithNoSections() {
        render(SettingsFixtures.emptyState())

        composeRule.onNodeWithTag(SettingsTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(SettingsTestTags.EMPTY_TITLE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.EMPTY_BODY, useUnmergedTree = true)
            .assertIsDisplayed()
        assertEquals(0, countOf(SettingsTestTags.SECTION))
        assertEquals(0, countOf(SettingsTestTags.ROW))
    }
}
