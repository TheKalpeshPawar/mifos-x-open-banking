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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.core.model.banking.payment.PaymentRail
import template.core.base.designsystem.theme.KptTheme

@Composable
fun RailToggle(
    rail: PaymentRail,
    onSelect: (PaymentRail) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(KptTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(PaymentRail.Domestic, PaymentRail.International).forEach { r ->
            val selected = r == rail
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        if (selected) {
                            KptTheme.colorScheme.primary
                        } else {
                            KptTheme.colorScheme.surfaceContainer
                        },
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onSelect(r) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (r == PaymentRail.Domestic) "Domestic" else "International",
                    style = KptTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) {
                        KptTheme.colorScheme.onPrimary
                    } else {
                        KptTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
