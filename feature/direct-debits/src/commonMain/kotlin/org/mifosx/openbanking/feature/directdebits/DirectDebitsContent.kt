/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.feature.directdebits.generated.resources.Res
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_card_accessibility
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_collection_time
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_last_collected
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_list_accessibility
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_status_accessibility
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitRowUi
import template.core.base.designsystem.theme.KptTheme

private const val INACTIVE_CARD_ALPHA = 0.82f

/**
 * The direct-debits body: a lazy list of one mandate card per instruction.
 *
 * The list is lazy because an account's mandate count is bank-decided and unbounded — OBIE returns
 * the full set in one unpaginated response, so the composition, not the request, is where this is
 * kept cheap.
 */
@Composable
internal fun DirectDebitsContent(
    mandates: List<DirectDebitRowUi>,
    modifier: Modifier = Modifier,
) {
    val listDescription = stringResource(Res.string.feature_direct_debits_list_accessibility)
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DirectDebitsTestTags.CONTENT)
            .semantics { contentDescription = listDescription },
        contentPadding = PaddingValues(KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        items(items = mandates, key = { it.mandateId.ifBlank { it.name } }) { mandate ->
            MandateCard(mandate = mandate)
        }
    }
}

/**
 * One mandate: originator, status badge, last collected amount and collection time.
 *
 * An inactive mandate is dimmed rather than hidden — a cancelled direct debit is information the
 * user came here for. The amount is deliberately neutral on-surface, never the error colour: a
 * direct debit is an expected outgoing, not a failure, and colouring it red would misreport a
 * healthy account as being in trouble.
 */
@Composable
private fun MandateCard(mandate: DirectDebitRowUi, modifier: Modifier = Modifier) {
    val cardDescription = stringResource(
        Res.string.feature_direct_debits_card_accessibility,
        mandate.name,
        mandate.statusLabel,
    )
    Surface(
        color = KptTheme.colorScheme.surfaceContainer,
        shape = KptTheme.shapes.medium,
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (mandate.isActive) 1f else INACTIVE_CARD_ALPHA)
            .testTag(DirectDebitsTestTags.card(mandate.mandateId))
            .semantics { contentDescription = cardDescription },
    ) {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            MandateCardHeader(mandate = mandate)
            MandateHistory(mandate = mandate)
        }
    }
}

@Composable
private fun MandateCardHeader(mandate: DirectDebitRowUi, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = mandate.name,
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        StatusBadge(mandate = mandate)
    }
}

/**
 * Active mandates get the filled primary-container badge; everything else gets the outline badge.
 *
 * Outline rather than a secondary fill: secondary reads as a second kind of emphasis, whereas an
 * inactive mandate needs less emphasis than an active one, and only an outline says that.
 */
@Composable
private fun StatusBadge(mandate: DirectDebitRowUi, modifier: Modifier = Modifier) {
    val shape = DesignToken.shapes.pill
    val description = stringResource(
        Res.string.feature_direct_debits_status_accessibility,
        mandate.statusLabel,
    )
    val base = modifier
        .testTag(DirectDebitsTestTags.statusBadge(mandate.mandateId))
        .semantics { contentDescription = description }
    Surface(
        color = if (mandate.isActive) KptTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = shape,
        modifier = if (mandate.isActive) {
            base
        } else {
            base.border(width = DesignToken.strokes.hairline, color = KptTheme.colorScheme.outline, shape = shape)
        },
    ) {
        Text(
            text = mandate.statusLabel,
            style = KptTheme.typography.labelSmall,
            color = if (mandate.isActive) {
                KptTheme.colorScheme.onPrimaryContainer
            } else {
                KptTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(
                horizontal = KptTheme.spacing.sm,
                vertical = KptTheme.spacing.xs,
            ),
        )
    }
}

/**
 * The last-collected amount and the collection time, shown for every mandate.
 */
@Composable
private fun MandateHistory(mandate: DirectDebitRowUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(Res.string.feature_direct_debits_last_collected),
                style = KptTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (mandate.isActive) {
                    KptTheme.colorScheme.onSurface
                } else {
                    KptTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = mandate.previousPaymentAmount,
                style = KptTheme.typography.titleSmall,
                color = if (mandate.isActive) {
                    KptTheme.colorScheme.onSurface
                } else {
                    KptTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag(DirectDebitsTestTags.amount(mandate.mandateId)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(Res.string.feature_direct_debits_collection_time),
                style = KptTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (mandate.isActive) {
                    KptTheme.colorScheme.onSurface
                } else {
                    KptTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = mandate.previousPaymentDateTime,
                style = KptTheme.typography.titleSmall,
                color = if (mandate.isActive) {
                    KptTheme.colorScheme.onSurface
                } else {
                    KptTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag(DirectDebitsTestTags.collectionTime(mandate.mandateId)),
            )
        }
    }
}
