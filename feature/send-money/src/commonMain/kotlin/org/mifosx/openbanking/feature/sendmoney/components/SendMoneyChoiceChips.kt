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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.banking.payment.ChargeBearer
import org.mifosx.openbanking.feature.sendmoney.SendMoneyTestTags
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_charge_bearer_creditor
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_charge_bearer_debtor
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_charge_bearer_service_level
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_charge_bearer_shared
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_currency_eur
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_currency_usd

private val ChipSpacing = 8.dp

/**
 * What the recipient is paid in.
 *
 * International only, and separate from the amount: the amount is instructed in sterling, and this
 * is the currency it arrives as. Only USD and EUR are offered because those are the two Global Money
 * accepts — anything else is refused with `U002`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecipientCurrencyPicker(
    selected: String,
    currencies: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().testTag(SendMoneyTestTags.CURRENCY_PICKER),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        currencies.forEach { code ->
            FilterChip(
                selected = code == selected,
                onClick = { onSelect(code) },
                label = { Text(stringResource(code.currencyLabel())) },
                modifier = Modifier.testTag(SendMoneyTestTags.currencyChip(code)),
            )
        }
    }
}

/**
 * Who pays the charges on an international payment.
 *
 * All four OBIE values are offered. Note only `BorneByCreditor` appears in HSBC's own sample
 * requests, so the other three are untested against the sandbox and may be refused at staging.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChargeBearerPicker(
    selected: ChargeBearer,
    onSelect: (ChargeBearer) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().testTag(SendMoneyTestTags.CHARGE_BEARER_PICKER),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        ChargeBearer.entries.forEach { bearer ->
            FilterChip(
                selected = bearer == selected,
                onClick = { onSelect(bearer) },
                label = { Text(stringResource(bearer.labelResource())) },
                modifier = Modifier.testTag(SendMoneyTestTags.chargeBearerChip(bearer)),
            )
        }
    }
}

/** Falls back to the bare code so an unexpected currency renders as itself rather than blank. */
private fun String.currencyLabel(): StringResource = when (this) {
    "USD" -> Res.string.feature_send_money_currency_usd
    else -> Res.string.feature_send_money_currency_eur
}

private fun ChargeBearer.labelResource(): StringResource = when (this) {
    ChargeBearer.BorneByCreditor -> Res.string.feature_send_money_charge_bearer_creditor
    ChargeBearer.BorneByDebtor -> Res.string.feature_send_money_charge_bearer_debtor
    ChargeBearer.Shared -> Res.string.feature_send_money_charge_bearer_shared
    ChargeBearer.FollowingServiceLevel -> Res.string.feature_send_money_charge_bearer_service_level
}
