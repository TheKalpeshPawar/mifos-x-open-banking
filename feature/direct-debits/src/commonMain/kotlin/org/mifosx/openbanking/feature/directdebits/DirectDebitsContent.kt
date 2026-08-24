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
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.feature.directdebits.generated.resources.Res
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_active_count
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_active_count_accessibility
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_amount_accessibility
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_card_accessibility
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_inactive_count
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_inactive_count_accessibility
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_last_collected
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_list_accessibility
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_mandate_reference
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_status_accessibility
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_summary_accessibility
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitRowUi
import template.core.base.designsystem.theme.KptTheme

private const val INACTIVE_CARD_ALPHA = 0.82f

/**
 * The direct-debits body: the two summary chips above the mandate list.
 *
 * The list is lazy because an account's mandate count is bank-decided and unbounded — OBIE returns
 * the full set in one unpaginated response, so the composition, not the request, is where this is
 * kept cheap.
 */
@Composable
internal fun DirectDebitsContent(
    mandates: List<DirectDebitRowUi>,
    activeCount: Int,
    inactiveCount: Int,
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
        item {
            MandateSummaryChips(activeCount = activeCount, inactiveCount = inactiveCount)
        }
        items(items = mandates, key = { it.mandateId.ifBlank { it.name } }) { mandate ->
            MandateCard(mandate = mandate)
        }
    }
}

/**
 * Display-only readouts of the active and inactive tallies.
 *
 * These are not filters. They carry no click handler by design — the list already shows both
 * groups, sorted, and a chip that looks tappable but is not would be worse than one that plainly
 * is not.
 */
@Composable
private fun MandateSummaryChips(
    activeCount: Int,
    inactiveCount: Int,
    modifier: Modifier = Modifier,
) {
    val groupDescription = stringResource(Res.string.feature_direct_debits_summary_accessibility)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DirectDebitsTestTags.SUMMARY_CHIPS)
            .semantics { contentDescription = groupDescription },
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        SummaryChip(
            label = stringResource(Res.string.feature_direct_debits_active_count, activeCount),
            description = stringResource(
                Res.string.feature_direct_debits_active_count_accessibility,
                activeCount,
            ),
            tonal = true,
            testTag = DirectDebitsTestTags.ACTIVE_CHIP,
        )
        SummaryChip(
            label = stringResource(Res.string.feature_direct_debits_inactive_count, inactiveCount),
            description = stringResource(
                Res.string.feature_direct_debits_inactive_count_accessibility,
                inactiveCount,
            ),
            tonal = false,
            testTag = DirectDebitsTestTags.INACTIVE_CHIP,
        )
    }
}

@Composable
private fun SummaryChip(
    label: String,
    description: String,
    tonal: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val shape = DesignToken.shapes.pill
    val base = modifier
        .testTag(testTag)
        .semantics { contentDescription = description }
    Surface(
        color = if (tonal) KptTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = shape,
        modifier = if (tonal) {
            base
        } else {
            base.border(width = DesignToken.strokes.hairline, color = KptTheme.colorScheme.outline, shape = shape)
        },
    ) {
        Text(
            text = label,
            style = KptTheme.typography.labelLarge,
            color = if (tonal) {
                KptTheme.colorScheme.onPrimaryContainer
            } else {
                KptTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(
                horizontal = KptTheme.spacing.md,
                vertical = KptTheme.spacing.xs,
            ),
        )
    }
}

/**
 * One mandate: originator, status badge, last collected amount, date and mandate reference.
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
            MandateAmount(mandate = mandate)
            MandateMetaLines(mandate = mandate)
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

@Composable
private fun MandateAmount(mandate: DirectDebitRowUi, modifier: Modifier = Modifier) {
    if (mandate.amountLabel.isBlank()) return
    val description = stringResource(
        Res.string.feature_direct_debits_amount_accessibility,
        mandate.amountLabel,
    )
    Text(
        text = mandate.amountLabel,
        style = KptTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
        color = if (mandate.isActive) {
            KptTheme.colorScheme.onSurface
        } else {
            KptTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
            .testTag(DirectDebitsTestTags.amount(mandate.mandateId))
            .semantics { contentDescription = description },
    )
}

/**
 * The last-collected date and the mandate reference, each omitted when the bank sent nothing for
 * it — a label with no value tells the user less than its absence does.
 */
@Composable
private fun MandateMetaLines(mandate: DirectDebitRowUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs)) {
        if (mandate.lastCollectedLabel.isNotBlank()) {
            Text(
                text = stringResource(
                    Res.string.feature_direct_debits_last_collected,
                    mandate.lastCollectedLabel,
                ),
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(DirectDebitsTestTags.lastCollected(mandate.mandateId)),
            )
        }
        if (mandate.mandateId.isNotBlank()) {
            Text(
                text = stringResource(
                    Res.string.feature_direct_debits_mandate_reference,
                    mandate.mandateId,
                ),
                style = KptTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = KptTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(DirectDebitsTestTags.mandateReference(mandate.mandateId)),
            )
        }
    }
}
