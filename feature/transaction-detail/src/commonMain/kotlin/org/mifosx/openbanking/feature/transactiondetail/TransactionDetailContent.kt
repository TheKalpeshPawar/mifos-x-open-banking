/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.Res
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_balance_after
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_bank_code
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_booking_date
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_card_title
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_category
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_category_dining
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_category_groceries
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_category_other
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_category_shopping
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_category_subscriptions
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_category_transfer
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_category_transport
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_copy_reference_a11y
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_mcc
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_reference
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_value_date
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailUiModel
import template.core.base.designsystem.theme.KptTheme

/**
 * The content state: an amount hero (colour-coded by credit/debit), the merchant and status, then a
 * grouped "Details" card of every supplementary field. The card scrolls; the header does not clip.
 */
@Composable
internal fun TransactionDetailContent(
    transaction: TransactionDetailUiModel,
    onCopyReference: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag(TransactionDetailTestTags.CONTENT_ROOT),
    ) {
        AmountHero(transaction)
        MerchantSection(transaction)
        HorizontalDivider(
            color = KptTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = KptTheme.spacing.md),
        )
        DetailCard(transaction = transaction, onCopyReference = onCopyReference)
    }
}

@Composable
private fun AmountHero(transaction: TransactionDetailUiModel) {
    Column(
        modifier = Modifier.padding(
            start = KptTheme.spacing.md,
            end = KptTheme.spacing.md,
            top = KptTheme.spacing.lg,
            bottom = KptTheme.spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = transaction.amountLabel,
            style = KptTheme.typography.displaySmall,
            fontFamily = FontFamily.Monospace,
            color = if (transaction.isCredit) {
                KptTheme.colorScheme.primary
            } else {
                KptTheme.colorScheme.error
            },
            modifier = Modifier.testTag(TransactionDetailTestTags.AMOUNT),
        )
        Text(
            text = transaction.currencyLabel,
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TransactionDetailTestTags.CURRENCY),
        )
    }
}

@Composable
private fun MerchantSection(transaction: TransactionDetailUiModel) {
    Column(
        modifier = Modifier.padding(
            start = KptTheme.spacing.md,
            end = KptTheme.spacing.md,
            bottom = KptTheme.spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        Text(
            text = transaction.merchantLabel,
            style = KptTheme.typography.headlineMedium,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(TransactionDetailTestTags.MERCHANT),
        )
        StatusChip(status = transaction.status, isBooked = transaction.isBooked)
    }
}

@Composable
private fun StatusChip(status: String, isBooked: Boolean) {
    val container = if (isBooked) {
        KptTheme.colorScheme.primaryContainer
    } else {
        KptTheme.colorScheme.secondaryContainer
    }
    val onContainer = if (isBooked) {
        KptTheme.colorScheme.onPrimaryContainer
    } else {
        KptTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = container,
        contentColor = onContainer,
        shape = KptTheme.shapes.small,
        modifier = Modifier.testTag(TransactionDetailTestTags.STATUS_CHIP),
    ) {
        Text(
            text = status,
            style = KptTheme.typography.labelLarge,
            modifier = Modifier
                .heightIn(min = KptTheme.spacing.xl)
                .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.xs),
        )
    }
}

@Composable
private fun DetailCard(
    transaction: TransactionDetailUiModel,
    onCopyReference: (String) -> Unit,
) {
    Surface(
        color = KptTheme.colorScheme.surfaceContainer,
        shape = KptTheme.shapes.medium,
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.md)
            .testTag(TransactionDetailTestTags.DETAIL_CARD),
    ) {
        Text(
            text = stringResource(Res.string.feature_transaction_detail_card_title),
            style = KptTheme.typography.titleSmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = KptTheme.spacing.md,
                end = KptTheme.spacing.md,
                top = KptTheme.spacing.md,
                bottom = KptTheme.spacing.sm,
            ),
        )
        SupportingRow(
            label = stringResource(Res.string.feature_transaction_detail_booking_date),
            supporting = transaction.bookingDateLabel,
            testTag = TransactionDetailTestTags.BOOKING_DATE_ROW,
        )
        InsetDivider()
        SupportingRow(
            label = stringResource(Res.string.feature_transaction_detail_value_date),
            supporting = transaction.valueDateLabel,
            testTag = TransactionDetailTestTags.VALUE_DATE_ROW,
        )
        InsetDivider()
        SupportingRow(
            label = stringResource(Res.string.feature_transaction_detail_category),
            supporting = stringResource(transaction.category.labelResource()),
            testTag = TransactionDetailTestTags.CATEGORY_ROW,
        )
        if (transaction.merchantCategoryCode != null) {
            InsetDivider()
            SupportingRow(
                label = stringResource(Res.string.feature_transaction_detail_mcc),
                supporting = transaction.merchantCategoryCode,
                testTag = TransactionDetailTestTags.MCC_ROW,
            )
        }
        InsetDivider()
        TrailingRow(
            label = stringResource(Res.string.feature_transaction_detail_balance_after),
            trailing = transaction.balanceAfterLabel,
            testTag = TransactionDetailTestTags.BALANCE_AFTER_ROW,
        )
        InsetDivider()
        ReferenceRow(
            reference = transaction.referenceLabel,
            onCopyReference = onCopyReference,
        )
        InsetDivider()
        SupportingRow(
            label = stringResource(Res.string.feature_transaction_detail_bank_code),
            supporting = transaction.bankCodeLabel,
            testTag = TransactionDetailTestTags.BANK_CODE_ROW,
        )
    }
}

/** A row whose value sits under the label as supporting text (dates, category, MCC, bank code). */
@Composable
private fun SupportingRow(label: String, supporting: String, testTag: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DesignToken.sizes.rowMin)
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.md)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = label,
            style = KptTheme.typography.bodyLarge,
            color = KptTheme.colorScheme.onSurface,
        )
        Text(
            text = supporting,
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A row whose value sits at the trailing edge in monospace (the running balance). */
@Composable
private fun TrailingRow(label: String, trailing: String, testTag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DesignToken.sizes.rowMin)
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.md)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = KptTheme.typography.bodyLarge,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = trailing,
            style = KptTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = KptTheme.colorScheme.onSurface,
        )
    }
}

/** The reference row: label and value on the left, a copy affordance on the right; the whole row copies. */
@Composable
private fun ReferenceRow(reference: String, onCopyReference: (String) -> Unit) {
    val label = stringResource(Res.string.feature_transaction_detail_reference)
    val copyHint = stringResource(Res.string.feature_transaction_detail_copy_reference_a11y)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DesignToken.sizes.rowMin)
            .clickable { onCopyReference(reference) }
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.md)
            .testTag(TransactionDetailTestTags.REFERENCE_ROW)
            .semantics { contentDescription = "$label: $reference. $copyHint" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = label,
                style = KptTheme.typography.bodyLarge,
                color = KptTheme.colorScheme.onSurface,
            )
            Text(
                text = reference,
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = null,
            tint = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(DesignToken.sizes.iconSmall)
                .testTag(TransactionDetailTestTags.REFERENCE_COPY_ICON),
        )
    }
}

@Composable
private fun InsetDivider() {
    HorizontalDivider(
        color = KptTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = KptTheme.spacing.md),
    )
}

private fun TransactionCategory.labelResource(): StringResource = when (this) {
    TransactionCategory.GROCERIES -> Res.string.feature_transaction_detail_category_groceries
    TransactionCategory.DINING -> Res.string.feature_transaction_detail_category_dining
    TransactionCategory.SUBSCRIPTIONS -> Res.string.feature_transaction_detail_category_subscriptions
    TransactionCategory.SHOPPING -> Res.string.feature_transaction_detail_category_shopping
    TransactionCategory.TRANSPORT -> Res.string.feature_transaction_detail_category_transport
    TransactionCategory.TRANSFER -> Res.string.feature_transaction_detail_category_transfer
    TransactionCategory.OTHER -> Res.string.feature_transaction_detail_category_other
}
