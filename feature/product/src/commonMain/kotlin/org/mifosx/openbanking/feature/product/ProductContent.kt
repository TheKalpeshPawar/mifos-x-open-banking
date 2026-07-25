/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.product.generated.resources.Res
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_credit_band_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_credit_interest_list_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_feature_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_features_list_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_header_card_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_id_label
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_monthly_max_charge_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_monthly_max_charge_label
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_overdraft_band_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_overdraft_ear
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_overdraft_list_a11y
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_overdraft_type_label
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_section_credit_interest
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_section_features
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_section_fees
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_section_overdraft
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_tier_aer
import org.mifosx.openbanking.feature.product.generated.resources.feature_product_tier_band_info
import org.mifosx.openbanking.feature.product.ui.CreditTierUiModel
import org.mifosx.openbanking.feature.product.ui.OverdraftTierUiModel
import org.mifosx.openbanking.feature.product.ui.ProductUiModel
import template.core.base.designsystem.theme.KptTheme

private val FeatureIconSize = 20.dp
private val TrailingFontSize = 15.sp

/**
 * Content state: the product identity card followed by the Fees, Credit Interest, Overdraft and Features
 * sections.
 *
 * Each section is omitted when the bank publishes nothing for it, so a product with no overdraft simply
 * has no Overdraft heading rather than an empty one.
 */
@Composable
internal fun ProductContent(
    product: ProductUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.md)
            .testTag(ProductTestTags.CONTENT),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        ProductHeaderCard(product = product)

        product.monthlyMaximumChargeLabel?.let { charge ->
            SectionHeader(
                label = stringResource(Res.string.feature_product_section_fees),
                modifier = Modifier.testTag(ProductTestTags.FEES_SECTION),
            )
            ListSection {
                TermRow(
                    label = stringResource(Res.string.feature_product_monthly_max_charge_label),
                    trailing = charge,
                    // The design tints a zero charge as primary — a fee-free account is good news.
                    trailingColor = if (product.isFeeFree) {
                        KptTheme.colorScheme.primary
                    } else {
                        KptTheme.colorScheme.onSurface
                    },
                    contentDescription = stringResource(
                        Res.string.feature_product_monthly_max_charge_a11y,
                        charge,
                    ),
                    modifier = Modifier.testTag(ProductTestTags.MONTHLY_MAX_CHARGE_ROW),
                )
            }
        }

        if (product.creditInterestTiers.isNotEmpty()) {
            SectionHeader(
                label = stringResource(Res.string.feature_product_section_credit_interest),
                modifier = Modifier.testTag(ProductTestTags.CREDIT_INTEREST_SECTION),
            )
            ListSection(
                contentDescription = stringResource(
                    Res.string.feature_product_credit_interest_list_a11y,
                ),
            ) {
                product.creditInterestTiers.forEachIndexed { index, tier ->
                    CreditTierRow(tier = tier, index = index)
                    if (index != product.creditInterestTiers.lastIndex) HorizontalDivider()
                }
            }
        }

        if (product.overdraftTiers.isNotEmpty()) {
            SectionHeader(
                label = stringResource(Res.string.feature_product_section_overdraft),
                modifier = Modifier.testTag(ProductTestTags.OVERDRAFT_SECTION),
            )
            ListSection(
                contentDescription = stringResource(Res.string.feature_product_overdraft_list_a11y),
            ) {
                product.overdraftTiers.forEachIndexed { index, tier ->
                    OverdraftTierRow(tier = tier, index = index)
                    if (index != product.overdraftTiers.lastIndex) HorizontalDivider()
                }
            }
        }

        if (product.features.isNotEmpty()) {
            SectionHeader(
                label = stringResource(Res.string.feature_product_section_features),
                modifier = Modifier.testTag(ProductTestTags.FEATURES_SECTION),
            )
            ListSection(
                contentDescription = stringResource(Res.string.feature_product_features_list_a11y),
            ) {
                product.features.forEachIndexed { index, feature ->
                    FeatureRow(feature = feature, index = index)
                }
            }
        }
    }
}

/** Identity card: the product type as a badge, the product name, and the product id for reference. */
@Composable
private fun ProductHeaderCard(
    product: ProductUiModel,
    modifier: Modifier = Modifier,
) {
    val cardLabel = stringResource(Res.string.feature_product_header_card_a11y)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KptTheme.colorScheme.surfaceContainerHigh, KptTheme.shapes.large)
            .padding(KptTheme.spacing.lg)
            .testTag(ProductTestTags.HEADER_CARD)
            .semantics { contentDescription = cardLabel },
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        // The mockup renders ProductType as a pill, not plain text — it reads as a classification
        // rather than a value.
        Text(
            text = product.productType,
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .background(KptTheme.colorScheme.secondaryContainer, KptTheme.shapes.small)
                .padding(horizontal = KptTheme.spacing.sm, vertical = KptTheme.spacing.xs)
                .testTag(ProductTestTags.TYPE_BADGE),
        )
        Text(
            text = product.productName,
            style = KptTheme.typography.headlineMedium,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(ProductTestTags.PRODUCT_NAME),
        )
        Text(
            text = stringResource(Res.string.feature_product_id_label, product.productId),
            style = KptTheme.typography.labelSmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(ProductTestTags.PRODUCT_ID),
        )
    }
}

@Composable
private fun SectionHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = KptTheme.typography.titleSmall,
        color = KptTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = KptTheme.spacing.sm),
    )
}

/** Grouping container for related rows, mirroring the mockup's `list-section` card. */
@Composable
private fun ListSection(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KptTheme.colorScheme.surfaceContainerLow, KptTheme.shapes.large)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        content = { content() },
    )
}

/**
 * One label/value row. Trailing values are monospaced so rates and amounts align down the column, which
 * is what the mockup does with its mono face.
 */
@Composable
private fun TermRow(
    label: String,
    trailing: String,
    trailingColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.md)
            .semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The label takes the slack and wraps; the rate must not, or "0.15% AER" breaks across lines and
        // stops reading as one value.
        Text(
            text = label,
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = trailing,
            style = KptTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = TrailingFontSize,
                fontWeight = FontWeight.Medium,
            ),
            color = trailingColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun CreditTierRow(tier: CreditTierUiModel, index: Int) {
    TermRow(
        label = stringResource(
            Res.string.feature_product_tier_band_info,
            tier.bandLimit,
            tier.applicationFrequency,
        ),
        trailing = stringResource(Res.string.feature_product_tier_aer, tier.aer),
        trailingColor = KptTheme.colorScheme.primary,
        contentDescription = stringResource(
            Res.string.feature_product_credit_band_a11y,
            tier.bandLimit,
            tier.aer,
            tier.applicationFrequency,
        ),
        modifier = Modifier.testTag(ProductTestTags.creditTier(index)),
    )
}

@Composable
private fun OverdraftTierRow(tier: OverdraftTierUiModel, index: Int) {
    TermRow(
        label = stringResource(Res.string.feature_product_overdraft_type_label, tier.overdraftType),
        trailing = stringResource(Res.string.feature_product_overdraft_ear, tier.ear),
        // Overdraft rates are a cost, so the mockup tints them with the error colour.
        trailingColor = KptTheme.colorScheme.error,
        contentDescription = stringResource(
            Res.string.feature_product_overdraft_band_a11y,
            tier.overdraftType,
            tier.ear,
        ),
        modifier = Modifier.testTag(ProductTestTags.overdraftTier(index)),
    )
}

@Composable
private fun FeatureRow(feature: String, index: Int) {
    val featureLabel = stringResource(Res.string.feature_product_feature_a11y, feature)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.sm)
            .testTag(ProductTestTags.feature(index))
            .semantics { contentDescription = featureLabel },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = KptTheme.colorScheme.primary,
            modifier = Modifier.size(FeatureIconSize),
        )
        Text(
            text = feature,
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = KptTheme.spacing.sm),
        )
    }
}
