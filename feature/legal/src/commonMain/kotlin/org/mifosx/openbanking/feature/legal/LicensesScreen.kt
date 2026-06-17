/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package org.mifosx.openbanking.feature.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Semantic test tags for the Licenses screen, mirroring the component ids declared in the
 * idea-layer spec (licenses/ui.yaml) so UI tests can address the title, subtitle, the app's
 * own license card, the library list, and each license group header.
 */
private object LicensesTestTags {
    const val TITLE = "licenses_title"
    const val SUBTITLE = "licenses_subtitle_text"
    const val APP_LICENSE = "licenses_app_license"
    const val LIBRARY_LIST = "licenses_list"
    const val GROUP_PREFIX = "licenses_group_"
}

/**
 * License statement for the application itself, shown above the third-party library list.
 */
private const val APP_LICENSE_NOTE =
    "Mifos X Open Banking is a free, open-source demo client maintained by the Mifos Initiative. " +
        "The app is licensed under the Mozilla Public License 2.0 and is built from the openMF " +
        "kmp-project-template. All accounts, transactions, and payments shown in the app come " +
        "from the Open Bank Project sandbox and exist for demonstration purposes only."

/**
 * Introductory line acknowledging the open-source dependencies listed below it.
 */
private const val LICENSES_SUBTITLE =
    "Mifos X Open Banking is built on these outstanding open-source libraries. Each entry " +
        "lists the project, the team that maintains it, and the license it is distributed under."

/**
 * Closing acknowledgement rendered after the last license group.
 */
private const val LICENSES_FOOTER =
    "Full license texts are available at each project's homepage. The Mifos Initiative thanks " +
        "every maintainer and contributor whose work makes this app possible."

/**
 * Libraries from [OPEN_SOURCE_LIBRARIES] grouped by license identifier, preserving the
 * declaration order of the source list for both groups and entries within a group.
 */
private val LIBRARIES_BY_LICENSE: Map<String, List<OpenSourceLibrary>> =
    OPEN_SOURCE_LIBRARIES.groupBy(OpenSourceLibrary::license)

/**
 * Open Source Licenses — static acknowledgement screen. Renders the application's own
 * MPL-2.0 license note followed by every bundled third-party library from
 * [OPEN_SOURCE_LIBRARIES], grouped by license, each row showing name, maintainer, project
 * URL, and a license badge. All content is baked in at build time; the screen performs no
 * I/O and holds no state. [onBack] is invoked from the top-bar navigation icon.
 */
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Open Source Licenses",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(LicensesTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(LicensesTestTags.LIBRARY_LIST),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        ) {
            item {
                Text(
                    LICENSES_SUBTITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(LicensesTestTags.SUBTITLE),
                )
                Spacer(Modifier.height(16.dp))
            }
            item {
                AppLicenseCard()
                Spacer(Modifier.height(8.dp))
            }
            LIBRARIES_BY_LICENSE.forEach { (license, libraries) ->
                item {
                    LicenseGroupHeader(license = license)
                }
                item {
                    LibraryGroupCard(libraries = libraries)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    LICENSES_FOOTER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Card stating the app's own MPL-2.0 license and its Open Bank Project sandbox provenance,
 * with the app name on the left and an MPL-2.0 badge on the right.
 */
@Composable
private fun AppLicenseCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LicensesTestTags.APP_LICENSE),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Mifos X Open Banking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                LicenseBadge(license = "MPL-2.0")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                APP_LICENSE_NOTE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Section header naming the license that every library in the following group is
 * distributed under. Tagged "[LicensesTestTags.GROUP_PREFIX]{license-id}" with dots and
 * dashes folded to underscores, e.g. licenses_group_apache_2_0.
 */
@Composable
private fun LicenseGroupHeader(license: String) {
    val tagSuffix = license.lowercase().replace('.', '_').replace('-', '_')
    Text(
        licenseDisplayName(license),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .testTag(LicensesTestTags.GROUP_PREFIX + tagSuffix),
    )
}

/**
 * Card listing one license group's libraries as two-line rows separated by inset dividers.
 */
@Composable
private fun LibraryGroupCard(libraries: List<OpenSourceLibrary>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            libraries.forEachIndexed { index, library ->
                LibraryRow(library = library)
                if (index < libraries.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Single library row: name, maintainer, and project URL stacked on the left with the
 * license badge trailing on the right.
 */
@Composable
private fun LibraryRow(library: OpenSourceLibrary) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                library.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                library.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                library.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        LicenseBadge(license = library.license)
    }
}

/**
 * Tonal pill displaying a license identifier on a primary-container surface.
 */
@Composable
private fun LicenseBadge(license: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            license,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * Maps an SPDX-style identifier from [OpenSourceLibrary.license] to the full license name
 * used as a section header, falling back to the identifier itself for unknown values.
 */
private fun licenseDisplayName(license: String): String = when (license) {
    "Apache-2.0" -> "Apache License 2.0"
    "MIT" -> "MIT License"
    "MPL-2.0" -> "Mozilla Public License 2.0"
    "EPL-1.0" -> "Eclipse Public License 1.0"
    else -> license
}
