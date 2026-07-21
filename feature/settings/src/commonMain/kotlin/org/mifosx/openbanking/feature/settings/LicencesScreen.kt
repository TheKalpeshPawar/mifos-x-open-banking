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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.settings.generated.resources.Res
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_licences_intro
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_licences_screen_title

private val SCREEN_PADDING = 16.dp
private val SECTION_GAP = 16.dp

/**
 * The open-source licences screen: the app's own licence.
 *
 * `mifos-x-open-banking` is released under the Mozilla Public License 2.0, and this screen shows that
 * licence's full text, read from the copy bundled in this module's Compose resources — the same text
 * the repository's `LICENSE` file carries.
 *
 * A single pushed screen rather than a tab, so it carries a navigation icon that returns to Settings
 * through [onBack].
 */
@Composable
internal fun LicencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val licence by produceState(initialValue = "") {
        value = Res.readBytes("files/mpl_licence.txt").decodeToString()
    }
    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_settings_licences_screen_title),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SCREEN_PADDING)
                .testTag(SettingsTestTags.LICENCES_SCREEN),
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        ) {
            Text(
                text = stringResource(Res.string.feature_settings_licences_intro),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = licence,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(SettingsTestTags.LICENCES_LIST),
            )
        }
    }
}
