/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.account

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_credit
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_current
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_global_money
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_global_wallet
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_other
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_savings

private const val LAST_DIGITS = 4
private const val TYPE_NUMBER_SEPARATOR = " ·· "

/**
 * The name to show for an account.
 *
 * A bank-provided `Nickname`/`Name` is used as-is. HSBC's sandbox provides neither (its top-level
 * `Description` is free text and the nested `Account[].Name` is the account holder, not the account),
 * so when [nickname] is blank this falls back to a localized account-type label plus the last four
 * digits of the account number — e.g. "Current account ·· 3349". Shared so Home, Accounts and
 * Account-detail render the same label from the same localized strings.
 *
 * @param accountNumber the flattened UK account number; [rawIdentification] (e.g. a card number or
 *   IBAN) is used for the last-four when the account number is absent, as it is for cards.
 */
@Composable
fun accountDisplayName(
    nickname: String,
    accountSubType: String,
    accountNumber: String,
    rawIdentification: String = "",
): String {
    if (nickname.isNotBlank()) return nickname
    val typeLabel = stringResource(accountTypeLabelRes(accountSubType))
    val lastDigits = accountNumber.ifBlank { rawIdentification }.takeLast(LAST_DIGITS)
    return if (lastDigits.isNotBlank()) "$typeLabel$TYPE_NUMBER_SEPARATOR$lastDigits" else typeLabel
}

/**
 * Maps an OBIE `AccountSubType` to its display label, accepting the enum case-insensitively and the
 * common ISO-20022 cash-account codes (`CACC`, `SVGS`, `CCRD`) so it resolves whether the bank
 * populates `AccountSubType` or only `AccountTypeCode`.
 */
private fun accountTypeLabelRes(accountSubType: String): StringResource = when (accountSubType.lowercase()) {
    "currentaccount", "current", "cacc" -> Res.string.core_ui_account_type_current
    "savings", "svgs" -> Res.string.core_ui_account_type_savings
    "creditcard", "credit", "card", "ccrd" -> Res.string.core_ui_account_type_credit
    "globalmoney" -> Res.string.core_ui_account_type_global_money
    "globalwallet" -> Res.string.core_ui_account_type_global_wallet
    else -> Res.string.core_ui_account_type_other
}
