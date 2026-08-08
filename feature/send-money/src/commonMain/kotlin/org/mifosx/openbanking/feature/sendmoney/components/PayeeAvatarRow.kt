/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.sendmoney.AvatarBadgeSize
import org.mifosx.openbanking.feature.sendmoney.AvatarGap
import org.mifosx.openbanking.feature.sendmoney.AvatarRing
import org.mifosx.openbanking.feature.sendmoney.AvatarRingGap
import org.mifosx.openbanking.feature.sendmoney.AvatarSize
import org.mifosx.openbanking.feature.sendmoney.HeadingGap
import org.mifosx.openbanking.feature.sendmoney.SendMoneyTestTags
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_manual_entry_a11y
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_payee_add_new
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyPickerRow
import template.core.base.designsystem.component.KptShimmerLoadingBox
import template.core.base.designsystem.theme.KptTheme

/** The ring is drawn in reserved space, so selecting cannot resize an avatar and reflow the row. */
private val SlotSize = AvatarSize + (AvatarRingGap + AvatarRing) * 2

/** Wide enough for two words of caption under the slot. */
private val CaptionWidth = SlotSize + 8.dp

private const val DASH_ON = 6f
private const val DASH_OFF = 5f

/** As many placeholders as the fixture account has payees, which is what the row usually returns. */
private const val PLACEHOLDER_COUNT = 3

/** A caption's own height, so the loading area can reserve one without measuring text. */
private val CaptionHeight = 14.dp

/**
 * One avatar row, floor to ceiling.
 *
 * The loading area is held to this so the section cannot collapse to nothing and then spring open
 * when the payees land, bouncing the amount field — the thing the customer is actually aiming at —
 * down the screen under their finger.
 *
 * `SendMoneySkeleton`'s `CreditorListHeight` is NOT the figure to reuse: that is the old
 * vertical-list block, and it is more than twice this.
 */
private val PayeeRowHeight = SlotSize + HeadingGap + CaptionHeight

/**
 * The payees, as a horizontal row of avatars.
 *
 * A vertical list of full-width rows was the wrong shape for this decision: a payee is recognised by
 * name in a glance, the list is short, and stacked rows pushed the amount — the thing the customer
 * actually came to type — off the screen. Scrolling sideways keeps the whole choice on one line.
 *
 * "Add new" leads rather than trailing, so the escape from an empty list is the first thing under
 * the heading rather than the last thing after a scroll. It opens the same manual-entry fields the
 * text button used to, and carries that button's test tag because it is the same affordance.
 *
 * When [loading], the placeholders are emitted here — inside this row, after "Add new" — and not as
 * a row of their own. Beneath it they took a second line, so the section stood taller while the read
 * was in flight and everything below jumped upward the moment the payees landed, which is the very
 * collapse the row's minimum height exists to prevent.
 */
@Composable
internal fun SendMoneyPayeeAvatarRow(
    payees: List<SendMoneyPickerRow>,
    selectedId: String?,
    selectedLabel: String,
    onSelect: (String) -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    loadingContentDescription: String = "",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .heightIn(min = PayeeRowHeight)
            .padding(vertical = HeadingGap),
        horizontalArrangement = Arrangement.spacedBy(AvatarGap),
    ) {
        AddNewAvatar(onClick = onAddNew)

        if (loading) {
            SendMoneyPayeeLoadingRow(
                contentDescription = loadingContentDescription,
                modifier = Modifier.testTag(SendMoneyTestTags.PAYEES_LOADING),
            )
        }

        payees.forEach { payee ->
            PayeeAvatar(
                payee = payee,
                selected = payee.id == selectedId,
                selectedLabel = selectedLabel,
                onClick = { onSelect(payee.id) },
                modifier = Modifier.testTag(SendMoneyTestTags.creditorRow(payee.id)),
            )
        }
    }
}

/**
 * The saved payees are still being read: placeholders shaped like the row that is coming.
 *
 * A shimmer and not a spinner, and the reason is written down in `SendMoneySkeleton`'s KDoc — "a
 * spinner here would be the one screen that loads differently from the rest". This screen already
 * shimmers while its accounts load; shimmering for its payees too is the same screen keeping its
 * word.
 *
 * Emitted INSIDE [SendMoneyPayeeAvatarRow], immediately after "Add new" — which is why it neither
 * replaces that row nor adds a second one. Replacing it would take away the only route to paying
 * someone unsaved for exactly as long as the customer is waiting to learn whether they need it;
 * sitting under it made the section a line taller while loading, so the form collapsed upward the
 * moment the payees arrived.
 *
 * @param contentDescription Resolved by the caller and applied here, because a shimmer has no text
 *   of its own: without this the whole state is silent to a screen reader, which is the same lie in
 *   a different medium.
 */
@Composable
internal fun SendMoneyPayeeLoadingRow(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(AvatarGap),
    ) {
        repeat(PLACEHOLDER_COUNT) {
            Column(
                modifier = Modifier.width(CaptionWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HeadingGap),
            ) {
                Box(
                    modifier = Modifier.size(SlotSize),
                    contentAlignment = Alignment.Center,
                ) {
                    KptShimmerLoadingBox(
                        modifier = Modifier.size(AvatarSize),
                        shape = CircleShape,
                    )
                }
                KptShimmerLoadingBox(
                    modifier = Modifier.fillMaxWidth().height(CaptionHeight),
                )
            }
        }
    }
}

@Composable
private fun AddNewAvatar(onClick: () -> Unit) {
    val outline = KptTheme.colorScheme.outline
    AvatarColumn(
        caption = stringResource(Res.string.feature_send_money_payee_add_new),
        captionColor = KptTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .testTag(SendMoneyTestTags.MANUAL_ENTRY_BUTTON)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .drawBehind {
                    val stroke = AvatarRing.toPx()
                    drawCircle(
                        color = outline,
                        radius = (size.minDimension - stroke) / 2,
                        style = Stroke(
                            width = stroke,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF)),
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(Res.string.feature_send_money_manual_entry_a11y),
                tint = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PayeeAvatar(
    payee: SendMoneyPickerRow,
    selected: Boolean,
    selectedLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ring = if (selected) {
        Modifier.border(AvatarRing, KptTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }
    AvatarColumn(
        caption = payee.shortName.ifBlank { payee.headline },
        captionColor = if (selected) {
            KptTheme.colorScheme.onSurface
        } else {
            KptTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
    ) {
        Box(
            modifier = Modifier.size(SlotSize).then(ring),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .background(
                        color = if (selected) {
                            KptTheme.colorScheme.primary
                        } else {
                            KptTheme.colorScheme.primaryContainer
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = payee.initials,
                    style = KptTheme.typography.titleMedium,
                    color = if (selected) {
                        KptTheme.colorScheme.onPrimary
                    } else {
                        KptTheme.colorScheme.onPrimaryContainer
                    },
                )
            }
            if (selected) {
                SelectedBadge(
                    selectedLabel = selectedLabel,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

/** The tick the reference hangs off the selected circle, so selection reads without relying on hue. */
@Composable
private fun SelectedBadge(selectedLabel: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AvatarBadgeSize)
            .background(KptTheme.colorScheme.primary, CircleShape)
            .border(AvatarRing, KptTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = selectedLabel,
            tint = KptTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * One column of the scroller: a ring-sized box with the avatar centred in it, and a caption beneath.
 *
 * The ring space is always reserved — [SlotSize] is bigger than the avatar by the ring and its gap —
 * so a selection ring appears inside space the layout has already given it, and nothing shifts under
 * the finger that is tapping.
 */
@Composable
private fun AvatarColumn(
    caption: String,
    captionColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.width(CaptionWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeadingGap),
    ) {
        Box(
            modifier = Modifier.size(SlotSize),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        Text(
            text = caption,
            style = KptTheme.typography.bodySmall,
            color = captionColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
