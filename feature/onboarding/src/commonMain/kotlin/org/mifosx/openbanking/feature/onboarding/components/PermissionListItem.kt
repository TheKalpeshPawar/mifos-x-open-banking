/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mifosx.openbanking.core.model.hsbcPermission.OBPermission
import org.mifosx.openbanking.core.model.hsbcPermission.PermissionId
import template.core.base.designsystem.theme.KptTheme

@Composable
fun PermissionListItem(permission: OBPermission, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = KptTheme.spacing.sm).testTag("onboarding_perm_${permission.id.name}"),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(KptTheme.spacing.sm)).background(KptTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(permissionIcon(permission.id), null, Modifier.size(20.dp), tint = KptTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(KptTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(permission.label, style = KptTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = KptTheme.colorScheme.onSurface)
                Spacer(Modifier.height(KptTheme.spacing.xs))
                Text(permission.description, style = KptTheme.typography.bodyMedium, color = KptTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(KptTheme.spacing.xs))
                Text(
                    permission.id.obieScope,
                    style = KptTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 0.3.sp),
                    color = KptTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(KptTheme.colorScheme.secondaryContainer).padding(horizontal = KptTheme.spacing.sm, vertical = 2.dp),
                )
            }
        }
        HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
    }
}

internal fun permissionIcon(id: PermissionId): ImageVector = when (id) {
    PermissionId.ReadAccountsBasic, PermissionId.ReadAccountsDetail -> Icons.Outlined.ManageAccounts
    PermissionId.ReadBalances -> Icons.Filled.AccountBalance
    PermissionId.ReadBeneficiariesBasic, PermissionId.ReadBeneficiariesDetail -> Icons.Outlined.Group
    PermissionId.ReadDirectDebits -> Icons.Outlined.Subscriptions
    PermissionId.ReadOffers -> Icons.Outlined.LocalOffer
    PermissionId.ReadPAN -> Icons.Outlined.CreditCard
    PermissionId.ReadParty -> Icons.Outlined.Person
    PermissionId.ReadProducts -> Icons.Outlined.Store
    PermissionId.ReadRefundAccount -> Icons.Outlined.Redeem
    PermissionId.ReadScheduledPaymentsBasic, PermissionId.ReadScheduledPaymentsDetail -> Icons.Outlined.Payment
    PermissionId.ReadStandingOrdersBasic -> Icons.Outlined.Refresh
    PermissionId.ReadStandingOrdersDetail -> Icons.Outlined.Autorenew
    PermissionId.ReadStatementsBasic, PermissionId.ReadStatementsDetail -> Icons.Outlined.Description
    PermissionId.ReadTransactionsBasic, PermissionId.ReadTransactionsDetail, PermissionId.ReadTransactionsCredits, PermissionId.ReadTransactionsDebits -> Icons.Outlined.ReceiptLong
}
