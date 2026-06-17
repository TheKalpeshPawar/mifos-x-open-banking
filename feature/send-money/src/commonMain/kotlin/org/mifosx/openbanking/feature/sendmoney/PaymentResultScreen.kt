/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.core.ui.card.HeroGradientCard

/**
 * Payment success screen reached after a payment books — both the immediately-settled path and the
 * post-SCA completion land here. A green gradient hero confirms the amount + payee, and a details
 * card lists the booked transaction. [onViewTransaction] opens the transaction; [onDone] returns home.
 */
@Composable
fun PaymentResultScreen(
    amount: String,
    currency: String,
    beneficiaryName: String,
    fromLabel: String,
    transactionId: String,
    chargeAmount: String,
    chargeCurrency: String,
    status: String,
    onViewTransaction: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PaymentResultHero(
                amount = amount,
                currency = currency,
                beneficiaryName = beneficiaryName,
                status = status,
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                val charge = "${currencySymbol(chargeCurrency.ifBlank { currency })}${chargeAmount.ifBlank { "0.00" }}"
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    ResultRow("Transaction ID", shortId(transactionId))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ResultRow("From", fromLabel.ifBlank { "—" })
                    ResultRow("Charge", charge)
                    ResultRow("Posted", "Just now")
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = onViewTransaction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("View transaction", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Done")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PaymentResultHero(
    amount: String,
    currency: String,
    beneficiaryName: String,
    status: String,
) {
    HeroGradientCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 72.dp, bottom = 36.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(92.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                ) {}
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.92f),
                ) {}
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }
            Text(
                text = "Payment sent",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${currencySymbol(currency)}$amount to ${beneficiaryName.ifBlank { "beneficiary" }}",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalContentColor.current.copy(alpha = 0.85f),
            )
            StatusPill(status.ifBlank { "COMPLETED" }.uppercase())
        }
    }
}

/** Translucent pill with a live-dot, e.g. "● COMPLETED" — rendered inside the gradient hero. */
@Composable
private fun StatusPill(status: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.18f)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFB6F2C0), CircleShape))
            Text(text = status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Shortens a long transaction UUID for display, e.g. "99888bf1-69aa-…". */
private fun shortId(id: String): String = when {
    id.isBlank() -> "—"
    id.length <= SHORT_ID_LENGTH -> id
    else -> id.take(SHORT_ID_LENGTH) + "…"
}

private const val SHORT_ID_LENGTH = 13
