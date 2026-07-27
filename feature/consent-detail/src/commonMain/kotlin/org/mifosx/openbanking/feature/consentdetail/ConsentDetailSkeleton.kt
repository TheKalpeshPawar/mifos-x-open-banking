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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.consentdetail.generated.resources.Res
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_loading_a11y
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_revoking_a11y
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_revoking_hint
import org.mifosx.openbanking.feature.consentdetail.generated.resources.feature_consent_detail_revoking_label

private val ContentPadding = 24.dp
private val LineGap = 20.dp
private val HintMaxWidth = 280.dp
private const val HINT_ALPHA = 0.8f

/** Loading state: a single centred spinner. */
@Composable
internal fun ConsentDetailSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_consent_detail_loading_a11y)
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(ConsentDetailTestTags.LOADING)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Revoking state: the DELETE is in flight.
 *
 * No consent content is rendered and no action is offered — the screen deliberately has nothing
 * tappable while the call is outstanding, which is what prevents a second DELETE. The spinner is
 * tinted with the error colour rather than the primary, so the user can see this is the destructive
 * operation running and not an ordinary load.
 */
@Composable
internal fun ConsentDetailRevoking(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_consent_detail_revoking_a11y)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(ConsentDetailTestTags.REVOKING_PROGRESS)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
        Text(
            text = stringResource(Res.string.feature_consent_detail_revoking_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(ConsentDetailTestTags.REVOKING_LABEL),
        )
        Text(
            text = stringResource(Res.string.feature_consent_detail_revoking_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HINT_ALPHA),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = HintMaxWidth),
        )
    }
}
