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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccountBalance
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Stable test tags for the About screen, keyed to the idea-layer test_context ids. */
private object AboutTags {
    const val TITLE = "about_title"
    const val LOGO_SECTION = "about_logo_section"
    const val APP_INFO_CARD = "about_app_info_card"
    const val PROJECT_CARD = "about_project_card"
    const val LEGAL_CARD = "about_legal_card"
    const val ATTRIBUTION = "about_attribution"
}

private const val MIFOS_URL = "https://mifos.org"
private const val OBP_URL = "https://openbankproject.com"
private const val GITHUB_URL = "https://github.com/openMF"

private const val APP_TAGLINE = "Open Banking for Everyone"

private const val WHAT_IS_THIS =
    "Mifos X Open Banking is an open-source demonstration client for the Open Bank " +
        "Project sandbox. It shows how a modern mobile banking experience — accounts, " +
        "payments, standing orders, and personal finance insights — can be built entirely " +
        "on open APIs, with every screen backed by the OBP sandbox rather than a " +
        "proprietary core."

private const val MIFOS_BLURB =
    "Built and maintained by the Mifos Initiative, a global community working to expand " +
        "financial inclusion through open-source banking technology. The app is part of " +
        "the Mifos X platform family and is published under the Mozilla Public License 2.0."

private const val OBP_BLURB =
    "All account, transaction, and counterparty data shown in this app comes from the " +
        "Open Bank Project sandbox — a free, hosted test environment that exposes " +
        "realistic banking data over the standard OBP REST API. No real money moves and " +
        "no real customer data is used."

private const val ATTRIBUTION_FOOTER =
    "Made with Kotlin Multiplatform and Compose Multiplatform by the Mifos community."

/**
 * Static About screen for the legal feature: app identity, version and build details,
 * project background with attribution to the Mifos Initiative and the Open Bank Project
 * sandbox, and tappable resource links opened through [LocalUriHandler].
 *
 * @param onBack invoked when the navigation icon is tapped.
 * @param appVersion display version string rendered in the App row; the host wires the
 * real build version, defaulting to the baseline release marker.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    appVersion: String = "1.0.0",
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(AboutTags.TITLE),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            AboutIdentitySection(appVersion = appVersion)
            Spacer(Modifier.height(8.dp))
            AboutAppInfoCard(appVersion = appVersion)
            Spacer(Modifier.height(16.dp))
            AboutProjectCard()
            Spacer(Modifier.height(16.dp))
            AboutLegalCard()
            Spacer(Modifier.height(24.dp))
            Text(
                ATTRIBUTION_FOOTER,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AboutTags.ATTRIBUTION),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Centered brand block: logo mark, app name, and tagline, with the version echoed below. */
@Composable
private fun AboutIdentitySection(appVersion: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .testTag(AboutTags.LOGO_SECTION),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                Icons.Outlined.AccountBalance,
                contentDescription = "Mifos X Open Banking logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(18.dp)
                    .size(44.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Mifos X Open Banking",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            APP_TAGLINE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Version $appVersion",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Card listing the version, build stamp, license, and platform rows for the current install. */
@Composable
private fun AboutAppInfoCard(appVersion: String) {
    AboutCard(modifier = Modifier.testTag(AboutTags.APP_INFO_CARD)) {
        AboutSectionHeader("App")
        AboutValueRow(label = "Version", value = appVersion)
        AboutRowDivider()
        AboutValueRow(label = "License", value = "MPL-2.0")
        AboutRowDivider()
        AboutValueRow(label = "Platform", value = "Kotlin Multiplatform")
    }
}

/** Card describing what the app is, who builds it, and where its sandbox data comes from. */
@Composable
private fun AboutProjectCard() {
    AboutCard(modifier = Modifier.testTag(AboutTags.PROJECT_CARD)) {
        AboutSectionHeader("About this app")
        AboutBodyText(WHAT_IS_THIS)
        Spacer(Modifier.height(12.dp))
        AboutSubheading("The Mifos Initiative")
        AboutBodyText(MIFOS_BLURB)
        Spacer(Modifier.height(12.dp))
        AboutSubheading("Open Bank Project sandbox")
        AboutBodyText(OBP_BLURB)
    }
}

/** Card with the legal and resource link rows; each row opens its destination URL externally. */
@Composable
private fun AboutLegalCard() {
    val uriHandler = LocalUriHandler.current
    AboutCard(modifier = Modifier.testTag(AboutTags.LEGAL_CARD)) {
        AboutSectionHeader("Legal & resources")
        AboutLinkRow(
            label = "Mifos Initiative",
            detail = "mifos.org",
            onClick = { uriHandler.openUri(MIFOS_URL) },
        )
        AboutRowDivider()
        AboutLinkRow(
            label = "Open Bank Project",
            detail = "openbankproject.com",
            onClick = { uriHandler.openUri(OBP_URL) },
        )
        AboutRowDivider()
        AboutLinkRow(
            label = "Source code on GitHub",
            detail = "github.com/openMF",
            onClick = { uriHandler.openUri(GITHUB_URL) },
        )
        AboutRowDivider()
        AboutLinkRow(
            label = "Mozilla Public License 2.0",
            detail = "mozilla.org/MPL/2.0",
            onClick = { uriHandler.openUri("https://mozilla.org/MPL/2.0/") },
        )
    }
}

/** Filled container card shared by every About section, on the surfaceContainer role. */
@Composable
private fun AboutCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

/** Primary-colored section title rendered at the top of an [AboutCard]. */
@Composable
private fun AboutSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** Smaller bold heading used to break the project card into attribution subsections. */
@Composable
private fun AboutSubheading(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/** Paragraph body text on the onSurfaceVariant role used inside section cards. */
@Composable
private fun AboutBodyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Label-value row for static metadata such as the version and build stamp. */
@Composable
private fun AboutValueRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Tappable row that opens an external destination, with a trailing open-in-new affordance. */
@Composable
private fun AboutLinkRow(
    label: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open $label",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Hairline divider between rows inside an [AboutCard], on the outlineVariant role. */
@Composable
private fun AboutRowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
