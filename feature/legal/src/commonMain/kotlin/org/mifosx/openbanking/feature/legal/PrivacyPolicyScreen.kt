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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Stable test tags for the Privacy Policy screen, mirroring the section ids
 * declared in the screen's idea-layer spec so UI tests address the same surfaces.
 */
private object PrivacyPolicyTags {
    const val SCREEN_TITLE = "privacy_policy_title"
    const val SANDBOX_BANNER = "pp_gdpr_banner"
    const val DATA_COLLECTION = "pp_data_collection_card"
    const val LAWFUL_BASIS = "pp_lawful_basis_card"
    const val PURPOSE = "pp_purpose_card"
    const val LOCAL_STORAGE = "pp_local_storage_card"
    const val DIAGNOSTICS = "pp_diagnostics_card"
    const val THIRD_PARTY = "pp_third_party_card"
    const val RETENTION = "pp_retention_card"
    const val USER_RIGHTS = "pp_user_rights_card"
    const val CONTACT = "pp_dpo_card"
    const val LAST_UPDATED = "pp_last_updated_text"
}

/**
 * One titled block of policy prose rendered as a filled section card.
 *
 * @property title Heading shown in the primary color role above the body.
 * @property body Complete policy text for the section.
 * @property tag Test tag applied to the section card surface.
 */
private data class PolicySection(
    val title: String,
    val body: String,
    val tag: String,
)

/** Banner copy declaring the sandbox-only scope of the app and the regulations this policy aligns with. */
private const val SANDBOX_NOTICE_TEXT =
    "Mifos X Open Banking is an open-source demonstration client for the Open Bank Project sandbox, " +
        "maintained by the community around the Mifos Initiative. Every account, balance and transaction " +
        "shown here is sandbox test data — the app never connects to a live bank. This policy describes, " +
        "in terms aligned with UK GDPR and EU GDPR (Regulation 2016/679), how the demo handles your data."

/** Date and version stamp for the currently bundled policy text. */
private const val LAST_UPDATED_STAMP = "Last updated: 28 May 2026 · Version 1.0"

/** Ordered policy sections baked into the build; the screen renders them verbatim without any data layer. */
private val privacyPolicySections = listOf(
    PolicySection(
        title = "Data This App Touches",
        tag = PrivacyPolicyTags.DATA_COLLECTION,
        body = "The app handles only what it needs to operate against the Open Bank Project sandbox: " +
            "(a) Sandbox credentials — the username and password you use to sign in to your sandbox " +
            "profile; (b) Sandbox banking data — demo accounts, balances, transactions, beneficiaries, " +
            "cards, standing orders and direct debits returned by the sandbox API; (c) App preferences — " +
            "theme, language and notification choices made on this device. The app never requests your " +
            "contacts, location, camera, microphone, photos or any other device data.",
    ),
    PolicySection(
        title = "Lawful Basis for Processing",
        tag = PrivacyPolicyTags.LAWFUL_BASIS,
        body = "Where GDPR applies, processing rests on: (a) Contract performance — exchanging your " +
            "sandbox credentials for a session token is necessary to deliver the sign-in you asked for; " +
            "(b) Legitimate interests — keeping the app stable through crash diagnostics that carry no " +
            "identifying data, balanced against your rights and freedoms; (c) Consent — for any optional " +
            "diagnostics setting you switch on, which you can withdraw at any time from Settings.",
    ),
    PolicySection(
        title = "How Your Data Is Used",
        tag = PrivacyPolicyTags.PURPOSE,
        body = "Your sandbox credentials are sent once at sign-in and exchanged for a short-lived session " +
            "token that authenticates every later API call; the password itself is never stored. Sandbox " +
            "banking data is fetched on demand and rendered on screen so you can explore the product " +
            "flows. Preferences are read solely to apply the appearance and behaviour you chose. Nothing " +
            "is profiled, scored, sold or used for advertising.",
    ),
    PolicySection(
        title = "Where Your Data Lives",
        tag = PrivacyPolicyTags.LOCAL_STORAGE,
        body = "Your session token and preferences are kept in app-private storage on this device, " +
            "protected by the operating system's application sandbox. Recently viewed sandbox data may " +
            "be cached locally so screens stay responsive when the connection drops. The app has no " +
            "backend of its own — nothing you do here is uploaded to servers run by the app's maintainers.",
    ),
    PolicySection(
        title = "Diagnostics & Crash Reporting",
        tag = PrivacyPolicyTags.DIAGNOSTICS,
        body = "Builds of this app may include a diagnostics module that records crash reports and " +
            "anonymous performance measurements, such as app start time and screen render duration, so " +
            "maintainers can find and fix defects. Diagnostic records never contain your sandbox " +
            "credentials, session token, account numbers or transaction details. When a build ships with " +
            "diagnostics enabled, a toggle in Settings lets you turn it off.",
    ),
    PolicySection(
        title = "Data Sharing",
        tag = PrivacyPolicyTags.THIRD_PARTY,
        body = "Data leaves this app in exactly one direction: API requests to the Open Bank Project " +
            "sandbox you signed in to. The sandbox operator processes those requests under its own " +
            "published terms at openbankproject.com. The app shares nothing with advertisers, data " +
            "brokers or social networks, and it embeds no third-party tracking SDKs.",
    ),
    PolicySection(
        title = "Data Retention",
        tag = PrivacyPolicyTags.RETENTION,
        body = "Sandbox banking data lives on the Open Bank Project sandbox and follows the sandbox " +
            "operator's own retention schedule — this app is a window onto it, never its custodian. The " +
            "local cache and your session token are cleared when you sign out, and uninstalling the app " +
            "removes everything it stored on this device. Crash reports, where enabled, are kept only as " +
            "long as needed to diagnose the defect they describe.",
    ),
    PolicySection(
        title = "Your Rights",
        tag = PrivacyPolicyTags.USER_RIGHTS,
        body = "Because personal data stays on your device, you can act on most rights directly: review " +
            "preferences in Settings, sign out to clear your session and cache, or uninstall to erase " +
            "all local data. Where GDPR or similar law applies you also hold the rights of access, " +
            "rectification, erasure, restriction of processing, data portability and objection. For data " +
            "held inside the sandbox itself, direct requests to the Open Bank Project, which operates " +
            "the sandbox as an independent service.",
    ),
    PolicySection(
        title = "Contact",
        tag = PrivacyPolicyTags.CONTACT,
        body = "Mifos X Open Banking is community-maintained open-source software associated with the " +
            "Mifos Initiative. Questions about this policy are best raised as an issue on the project's " +
            "GitHub repository, or by email to privacy@mifos.org. Concerns about sandbox-side processing " +
            "can be sent to the Open Bank Project via openbankproject.com. If you believe your " +
            "data-protection rights have been infringed, you may also complain to your local supervisory " +
            "authority.",
    ),
)

/**
 * Static Privacy Policy screen for the Mifos X Open Banking demo client.
 *
 * Renders the bundled policy prose — sandbox notice banner, titled section cards and a
 * last-updated footer — inside a scrollable column. All content is baked into the build,
 * so the screen needs no view model, dependency injection or network access.
 *
 * @param onBack Invoked when the navigation icon in the top app bar is tapped.
 * @param modifier Applied to the root [Scaffold].
 */
@Composable
fun PrivacyPolicyScreen(
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
                        "Privacy Policy",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(PrivacyPolicyTags.SCREEN_TITLE),
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SandboxNoticeBanner()
            privacyPolicySections.forEach { section -> PolicySectionCard(section) }
            LastUpdatedFooter()
        }
    }
}

/** Prominent banner stating that the app is a sandbox-only demo, shown above all policy sections. */
@Composable
private fun SandboxNoticeBanner() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PrivacyPolicyTags.SANDBOX_BANNER),
    ) {
        Text(
            SANDBOX_NOTICE_TEXT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/**
 * Filled card rendering one [PolicySection] as a primary-colored heading over body prose.
 *
 * @param section Section whose title, body and test tag are rendered.
 */
@Composable
private fun PolicySectionCard(section: PolicySection) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(section.tag),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                section.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Centered footer carrying the policy's date and version stamp. */
@Composable
private fun LastUpdatedFooter() {
    Text(
        LAST_UPDATED_STAMP,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp)
            .testTag(PrivacyPolicyTags.LAST_UPDATED),
    )
}
