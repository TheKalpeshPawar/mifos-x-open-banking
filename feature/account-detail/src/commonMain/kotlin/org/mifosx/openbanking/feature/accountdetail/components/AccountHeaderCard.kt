/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.common.formatAccountIdentifier
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.ui.account.accountDisplayName
import org.mifosx.openbanking.feature.accountdetail.AccountDetailTestTags
import org.mifosx.openbanking.feature.accountdetail.generated.resources.Res
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_header_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_last_updated
import org.mifosx.openbanking.feature.accountdetail.ui.formatStatusTimestamp

private val CARD_RADIUS = 12.dp
private val CARD_PADDING = 16.dp
private val BADGE_RADIUS = 8.dp
private val SUBTYPE_TRACKING = 0.6.sp

/**
 * Identity card at the top of the account-detail screen: account type, account holder name, the
 * identification, currency and servicer badges, and the status timestamp.
 *
 * The identification is monospaced so digits align, and the subtype is upper-cased with wide
 * tracking to read as a label rather than a heading.
 */
@Composable
internal fun AccountHeaderCard(detail: AccountDetail, modifier: Modifier = Modifier) {
    val cardDescription = stringResource(Res.string.feature_account_detail_header_accessibility)
    val lastUpdatedLabel = formatStatusTimestamp(detail.statusUpdateDateTime)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(CARD_RADIUS),
        modifier = modifier
            .fillMaxWidth()
            .testTag(AccountDetailTestTags.HEADER_CARD)
            .semantics { contentDescription = cardDescription },
    ) {
        Column(
            modifier = Modifier.padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = detail.accountTypeCode.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = SUBTYPE_TRACKING),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag(AccountDetailTestTags.SUBTYPE_LABEL),
            )
            Text(
                text = accountDisplayName(
                    accountHolderName = detail.accountHolderName,
                    accountTypeCode = detail.accountTypeCode,
                    scheme = detail.scheme,
                    identification = detail.identification,
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(AccountDetailTestTags.DISPLAY_NAME),
            )
            Text(
                text = formatAccountIdentifier(detail.scheme, detail.identification),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(AccountDetailTestTags.IDENTIFICATION),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(BADGE_RADIUS)) {
                MetaBadge(
                    text = detail.currency,
                    testTag = AccountDetailTestTags.CURRENCY_BADGE,
                )
                if (detail.servicerIdentification.isNotBlank()) {
                    MetaBadge(
                        text = detail.servicerIdentification,
                        testTag = AccountDetailTestTags.SERVICER_BADGE,
                        monospace = true,
                    )
                }
            }
            if (lastUpdatedLabel.isNotBlank()) {
                Text(
                    text = "${stringResource(Res.string.feature_account_detail_last_updated)} " +
                        lastUpdatedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(AccountDetailTestTags.LAST_UPDATED),
                )
            }
        }
    }
}

@Composable
private fun MetaBadge(
    text: String,
    testTag: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(BADGE_RADIUS),
        modifier = modifier.testTag(testTag),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = BADGE_RADIUS, vertical = 2.dp),
        )
    }
}
