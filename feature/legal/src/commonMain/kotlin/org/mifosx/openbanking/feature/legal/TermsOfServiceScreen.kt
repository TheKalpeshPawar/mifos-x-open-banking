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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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

/** Stable test tags for the Terms of Service screen, keyed to the idea-layer test_context ids. */
private object TosTags {
    const val TITLE = "tos_title"
    const val LAST_UPDATED = "tos_last_updated_text"
}

/**
 * One clause of the terms: a stable [tag] for UI tests, the section [title] (numbered for
 * every clause after the overview), and the full clause [body] rendered as one paragraph.
 */
private data class TosSection(
    val tag: String,
    val title: String,
    val body: String,
)

private const val TOS_LAST_UPDATED = "Last updated: 28 May 2026 — Version 1.0"

/** Ordered clauses of the agreement: an un-numbered overview followed by ten numbered terms. */
private val tosSections = listOf(
    TosSection(
        tag = "tos_intro_card",
        title = "Agreement Overview",
        body = "These Terms of Service govern your use of Mifos X Open Banking, an open-source " +
            "demonstration client built by the Mifos Initiative community on top of the Open Bank " +
            "Project sandbox. Please read them carefully: they explain what this app is, what it " +
            "is not, and the rules that apply when you use it.",
    ),
    TosSection(
        tag = "tos_acceptance_card",
        title = "1. Acceptance of Terms",
        body = "By installing the app, signing in with sandbox DirectLogin credentials, or " +
            "otherwise using Mifos X Open Banking, you confirm that you have read, understood, " +
            "and agree to be bound by these terms. If you do not agree, do not use the app. If " +
            "you use the app on behalf of an organisation, you confirm that you are authorised " +
            "to accept these terms for that organisation.",
    ),
    TosSection(
        tag = "tos_demo_service_card",
        title = "2. A Demonstration Service, Not a Real Bank",
        body = "Mifos X Open Banking is a demonstration application. It is not a bank, an " +
            "e-money institution, or any other regulated financial service, and it never holds, " +
            "moves, or safeguards real money. Every account, balance, transaction, and " +
            "counterparty shown in the app is synthetic data hosted on the Open Bank Project " +
            "sandbox. Sandbox accounts and data may be reset, changed, or deleted at any time " +
            "without notice, and no balance shown in the app is covered by any deposit " +
            "guarantee scheme.",
    ),
    TosSection(
        tag = "tos_account_usage_card",
        title = "3. Accounts and Credentials",
        body = "Access is provided through DirectLogin credentials issued for the Open Bank " +
            "Project sandbox. Keep these credentials confidential, and never reuse a password " +
            "from a real banking or financial service. Tell the project maintainers promptly if " +
            "you suspect your credentials have been compromised. Sandbox access that disrupts " +
            "the service for other users may be suspended or revoked.",
    ),
    TosSection(
        tag = "tos_prohibited_card",
        title = "4. Acceptable Use",
        body = "You agree not to: (a) attack, overload, or attempt to circumvent the security " +
            "controls or rate limits of the Open Bank Project sandbox; (b) harvest or scrape " +
            "data belonging to other sandbox users; (c) present the app or its synthetic data " +
            "as a real banking service in order to mislead anyone; (d) introduce malicious code " +
            "into the app or its backing services; or (e) use the app for any unlawful purpose.",
    ),
    TosSection(
        tag = "tos_licensing_card",
        title = "5. Open-Source Licensing and Intellectual Property",
        body = "The source code of Mifos X Open Banking is licensed under the Mozilla Public " +
            "License, v. 2.0. A copy of the MPL is available at https://mozilla.org/MPL/2.0/. " +
            "Your rights to use, modify, and redistribute the code are granted by that license " +
            "rather than by these terms. The Mifos name and logo remain marks of the Mifos " +
            "Initiative, and bundled third-party components remain subject to their own " +
            "licenses, including the Open Bank Project API terms.",
    ),
    TosSection(
        tag = "tos_data_handling_card",
        title = "6. Sandbox Data",
        body = "Anything you enter into the app is stored on the Open Bank Project sandbox and " +
            "should be treated as non-confidential test data. Do not enter real personal " +
            "details, real account numbers, or real payment credentials. Periodic sandbox " +
            "resets permanently remove stored data, and the maintainers cannot recover anything " +
            "lost in a reset. How the app handles the limited data it does process is described " +
            "in the Privacy Policy.",
    ),
    TosSection(
        tag = "tos_warranty_card",
        title = "7. Disclaimer of Warranty",
        body = "The app and the sandbox services behind it are provided \"as is\" and \"as " +
            "available\", without warranty of any kind, whether express, implied, or statutory. " +
            "This includes, without limitation, any warranty of merchantability, fitness for a " +
            "particular purpose, availability, or accuracy of data, and mirrors the warranty " +
            "disclaimer in Section 6 of the MPL-2.0.",
    ),
    TosSection(
        tag = "tos_liability_card",
        title = "8. Limitation of Liability",
        body = "To the maximum extent permitted by applicable law, the Mifos Initiative, the " +
            "project contributors, and the operators of the Open Bank Project sandbox are not " +
            "liable for any indirect, incidental, special, consequential, or punitive damages, " +
            "or for any loss of data, arising from your use of this demonstration app. Because " +
            "the app never holds real funds, no claim can arise for monetary loss in respect of " +
            "sandbox balances or transactions.",
    ),
    TosSection(
        tag = "tos_changes_card",
        title = "9. Changes to These Terms",
        body = "These terms may be revised as the demonstration evolves. Material changes are " +
            "announced in the app and in the project repository, and the version stamp at the " +
            "bottom of this screen is updated with each revision. Continuing to use the app " +
            "after a revision takes effect constitutes acceptance of the revised terms.",
    ),
    TosSection(
        tag = "tos_contact_card",
        title = "10. Contact",
        body = "Questions about these terms are welcome through the Mifos Initiative community " +
            "at mifos.org, by opening an issue on the project's GitHub repository, or by " +
            "emailing legal@mifos.org. The community aims to respond within ten working days.",
    ),
)

/**
 * Static Terms of Service screen for the legal feature: an agreement overview followed by ten
 * numbered clauses covering acceptance, the demonstration-only nature of the app, credentials,
 * acceptable use, MPL-2.0 licensing, sandbox data, warranty, liability, changes, and contact,
 * closed by a last-updated version stamp. All content is baked in; nothing is loaded.
 *
 * @param onBack invoked when the navigation icon is tapped.
 */
@Composable
fun TermsOfServiceScreen(
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
                        "Terms of Service",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(TosTags.TITLE),
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
            Spacer(Modifier.height(8.dp))
            tosSections.forEach { section ->
                TosSectionCard(section)
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                TOS_LAST_UPDATED,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TosTags.LAST_UPDATED),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Filled card on the surfaceContainer role rendering one [TosSection]: its primary-colored
 * [TosSection.title] heading above the [TosSection.body] paragraph, tagged with [TosSection.tag].
 */
@Composable
private fun TosSectionCard(section: TosSection) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
