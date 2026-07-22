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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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

private val ScreenPadding = 16.dp
private val HeroTopPadding = 24.dp
private val SectionGap = 16.dp
private val ChipHeight = 32.dp
private val ChipHorizontalPadding = 12.dp
private val CardCornerRadius = 12.dp
private val ChipCornerRadius = 8.dp
private val RowMinHeight = 48.dp
private val RowVerticalPadding = 12.dp
private val RowTextGap = 2.dp
private val CardHeaderTopPadding = 16.dp
private val CardHeaderBottomPadding = 8.dp
private val CopyIconSize = 20.dp
private val DividerInsetStart = 16.dp
private val ElementGap = 12.dp

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
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = ScreenPadding),
        )
        DetailCard(transaction = transaction, onCopyReference = onCopyReference)
    }
}

@Composable
private fun AmountHero(transaction: TransactionDetailUiModel) {
    Column(
        modifier = Modifier.padding(
            start = ScreenPadding,
            end = ScreenPadding,
            top = HeroTopPadding,
            bottom = SectionGap,
        ),
        verticalArrangement = Arrangement.spacedBy(RowTextGap),
    ) {
        Text(
            text = transaction.amountLabel,
            style = MaterialTheme.typography.displaySmall,
            fontFamily = FontFamily.Monospace,
            color = if (transaction.isCredit) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.testTag(TransactionDetailTestTags.AMOUNT),
        )
        Text(
            text = transaction.currencyLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TransactionDetailTestTags.CURRENCY),
        )
    }
}

@Composable
private fun MerchantSection(transaction: TransactionDetailUiModel) {
    Column(
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = SectionGap),
        verticalArrangement = Arrangement.spacedBy(ElementGap),
    ) {
        Text(
            text = transaction.merchantLabel,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(TransactionDetailTestTags.MERCHANT),
        )
        StatusChip(status = transaction.status, isBooked = transaction.isBooked)
    }
}

@Composable
private fun StatusChip(status: String, isBooked: Boolean) {
    val container = if (isBooked) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer = if (isBooked) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = container,
        contentColor = onContainer,
        shape = RoundedCornerShape(ChipCornerRadius),
        modifier = Modifier.testTag(TransactionDetailTestTags.STATUS_CHIP),
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .heightIn(min = ChipHeight)
                .padding(horizontal = ChipHorizontalPadding, vertical = RowTextGap),
        )
    }
}

@Composable
private fun DetailCard(
    transaction: TransactionDetailUiModel,
    onCopyReference: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(CardCornerRadius),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = SectionGap)
            .testTag(TransactionDetailTestTags.DETAIL_CARD),
    ) {
        Text(
            text = stringResource(Res.string.feature_transaction_detail_card_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = ScreenPadding,
                end = ScreenPadding,
                top = CardHeaderTopPadding,
                bottom = CardHeaderBottomPadding,
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
            .heightIn(min = RowMinHeight)
            .padding(horizontal = ScreenPadding, vertical = RowVerticalPadding)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(RowTextGap),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A row whose value sits at the trailing edge in monospace (the running balance). */
@Composable
private fun TrailingRow(label: String, trailing: String, testTag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(horizontal = ScreenPadding, vertical = RowVerticalPadding)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
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
            .heightIn(min = RowMinHeight)
            .clickable { onCopyReference(reference) }
            .padding(horizontal = ScreenPadding, vertical = RowVerticalPadding)
            .testTag(TransactionDetailTestTags.REFERENCE_ROW)
            .semantics { contentDescription = "$label: $reference. $copyHint" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RowTextGap),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = reference,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(CopyIconSize)
                .testTag(TransactionDetailTestTags.REFERENCE_COPY_ICON),
        )
    }
}

@Composable
private fun InsetDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = DividerInsetStart),
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
