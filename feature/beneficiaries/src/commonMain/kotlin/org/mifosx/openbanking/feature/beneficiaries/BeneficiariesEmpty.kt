/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.Res
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_empty_a11y
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_empty_body
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_empty_title
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_search_empty_a11y
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_search_empty_body
import org.mifosx.openbanking.feature.beneficiaries.generated.resources.feature_beneficiaries_search_empty_title

private val IconWellSize = 72.dp
private val IconSize = 40.dp
private val BodyMaxWidth = 280.dp
private val ContentPadding = 24.dp
private val LineGap = 8.dp

/**
 * Empty state: an account the bank reports no saved payees for.
 *
 * Distinct from the error state by design — this is a successful answer, so it uses the neutral
 * primary-container well rather than the error container. It is also distinct from the no-results
 * block below: this one means the account has no payees at all, and the search field is not shown
 * because there is nothing to filter.
 */
@Composable
internal fun BeneficiariesEmpty(modifier: Modifier = Modifier) {
    EmptyBlock(
        icon = Icons.Outlined.People,
        title = stringResource(Res.string.feature_beneficiaries_empty_title),
        body = stringResource(Res.string.feature_beneficiaries_empty_body),
        description = stringResource(Res.string.feature_beneficiaries_empty_a11y),
        rootTag = BeneficiariesTestTags.EMPTY_STATE,
        titleTag = BeneficiariesTestTags.EMPTY_TITLE,
        bodyTag = BeneficiariesTestTags.EMPTY_BODY,
        modifier = modifier,
    )
}

/**
 * No-results block, rendered *inside* the content state when a live query matches nothing.
 *
 * Deliberately not a screen state: the query is still live and the unfiltered list is still held, so
 * clearing the field brings every row straight back. Falling into [BeneficiariesEmpty] here would
 * tell the user the account has no payees, which is a different and untrue claim.
 */
@Composable
internal fun BeneficiariesSearchEmpty(modifier: Modifier = Modifier) {
    EmptyBlock(
        icon = Icons.Outlined.SearchOff,
        title = stringResource(Res.string.feature_beneficiaries_search_empty_title),
        body = stringResource(Res.string.feature_beneficiaries_search_empty_body),
        description = stringResource(Res.string.feature_beneficiaries_search_empty_a11y),
        rootTag = BeneficiariesTestTags.SEARCH_EMPTY_STATE,
        titleTag = BeneficiariesTestTags.SEARCH_EMPTY_TITLE,
        bodyTag = BeneficiariesTestTags.SEARCH_EMPTY_BODY,
        modifier = modifier,
    )
}

@Composable
private fun EmptyBlock(
    icon: ImageVector,
    title: String,
    body: String,
    description: String,
    rootTag: String,
    titleTag: String,
    bodyTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .testTag(rootTag)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconWellSize)
                .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(titleTag),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = BodyMaxWidth)
                .testTag(bodyTag),
        )
    }
}
