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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.common.formatAccountIdentifier
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.ui.account.accountDisplayName
import org.mifosx.openbanking.feature.accountdetail.AccountDetailTestTags
import org.mifosx.openbanking.feature.accountdetail.generated.resources.Res
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_header_accessibility
import template.core.base.designsystem.theme.KptTheme

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
    Surface(
        color = KptTheme.colorScheme.surfaceContainer,
        shape = KptTheme.shapes.medium,
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag(AccountDetailTestTags.HEADER_CARD)
            .semantics { contentDescription = cardDescription },
    ) {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = detail.accountTypeCode.uppercase(),
                style = KptTheme.typography.labelMedium.copy(letterSpacing = SUBTYPE_TRACKING),
                color = KptTheme.colorScheme.secondary,
                modifier = Modifier.testTag(AccountDetailTestTags.SUBTYPE_LABEL),
            )
            Text(
                text = accountDisplayName(
                    accountHolderName = detail.accountHolderName,
                    accountTypeCode = detail.accountTypeCode,
                    scheme = detail.scheme,
                    identification = detail.identification,
                ),
                style = KptTheme.typography.headlineMedium,
                color = KptTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(AccountDetailTestTags.DISPLAY_NAME),
            )
            Text(
                text = formatAccountIdentifier(detail.scheme, detail.identification),
                style = KptTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = KptTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(AccountDetailTestTags.IDENTIFICATION),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
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
        color = KptTheme.colorScheme.surfaceContainerHigh,
        shape = KptTheme.shapes.small,
        modifier = modifier.testTag(testTag),
    ) {
        Text(
            text = text,
            style = KptTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            ),
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = KptTheme.spacing.sm, vertical = KptTheme.spacing.xs),
        )
    }
}
