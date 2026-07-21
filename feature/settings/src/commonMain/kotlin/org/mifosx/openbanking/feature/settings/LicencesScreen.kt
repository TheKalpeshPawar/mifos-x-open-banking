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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.rememberLibraries
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.settings.generated.resources.Res
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_licences_screen_title

/**
 * The open-source licences screen. Reads the attribution catalogue that the AboutLibraries Gradle
 * plugin exports into this module's Compose resources at build time, and lists every dependency and
 * its licence in a scrolling container.
 *
 * A single pushed screen rather than a tab, so it carries a navigation icon that returns to Settings
 * through [onBack].
 */
@Composable
internal fun LicencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val libraries by rememberLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }
    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_settings_licences_screen_title),
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize().testTag(SettingsTestTags.LICENCES_SCREEN)) {
            libraries?.let { libs ->
                LibrariesContainer(
                    libs,
                    modifier = Modifier.fillMaxSize().testTag(SettingsTestTags.LICENCES_LIST),
                )
            }
        }
    }
}
