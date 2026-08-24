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
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_credit
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_current
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_global_money
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_global_wallet
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_other
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_savings

private const val LAST_DIGITS = 4
private const val TYPE_NUMBER_SEPARATOR = " ·· "
private const val TYPE_CARD_SEPARATOR = " "
private const val MASK_CHARACTER = "X"
private val CREDIT_CARD_TYPE_CODES = setOf("card", "ccrd")

/**
 * The account's number with every character but the last four masked, e.g. `XXXX3349`.
 *
 * A card's identification is already masked by the bank, so it is shown as-is. Blank when the bank
 * supplied no identifier at all.
 */
fun maskedAccountNumber(scheme: AccountScheme, identification: String): String {
    if (scheme == AccountScheme.Pan) {
        return identification
    }
    val digits = identification.filter { it.isLetterOrDigit() }
    return if (digits.length <= LAST_DIGITS) {
        digits
    } else {
        MASK_CHARACTER.repeat(digits.length - LAST_DIGITS) + digits.takeLast(LAST_DIGITS)
    }
}

/** The localized account-type label on its own, e.g. `Current account`. */
@Composable
fun accountTypeLabel(accountTypeCode: String): String =
    stringResource(accountTypeLabelRes(accountTypeCode))

/**
 * The name to show for an account.
 *
 * [accountHolderName] (OBIE nested `Account[].Name`) is used as-is when the bank supplies one; when it
 * is blank this falls back to a localized account-type label plus an identifier — e.g.
 * "Current account ·· 3349". Shared so Home, Accounts and Account-detail render the same label from the
 * same localized strings.
 */
@Composable
fun accountDisplayName(
    accountHolderName: String,
    accountTypeCode: String,
    scheme: AccountScheme,
    identification: String,
): String {
    if (accountHolderName.isNotBlank()) return accountHolderName
    val typeLabel = stringResource(accountTypeLabelRes(accountTypeCode))
    return accountFallbackLabel(typeLabel, accountTypeCode, scheme, identification)
}

/**
 * Builds the "type + identifier" fallback shown when the bank supplied no holder name.
 *
 * A card's identification is already masked by the bank, so it is shown as-is after the type label.
 * Every other product keeps the "type ·· last 4" form — e.g. "Current account ·· 3349".
 *
 * Non-composable so it is unit-testable on the JVM without a Compose runtime.
 */
internal fun accountFallbackLabel(
    typeLabel: String,
    accountTypeCode: String,
    scheme: AccountScheme,
    identification: String,
): String {
    if (isCreditCard(scheme, accountTypeCode)) {
        return if (identification.isNotBlank()) {
            "$typeLabel$TYPE_CARD_SEPARATOR$identification"
        } else {
            typeLabel
        }
    }
    val lastDigits = identification.takeLast(LAST_DIGITS)
    return if (lastDigits.isNotBlank()) "$typeLabel$TYPE_NUMBER_SEPARATOR$lastDigits" else typeLabel
}

/** True for an account the display treats as a credit card. */
internal fun isCreditCard(scheme: AccountScheme, accountTypeCode: String): Boolean =
    scheme == AccountScheme.Pan || accountTypeCode.lowercase() in CREDIT_CARD_TYPE_CODES

/**
 * Maps an OBIE `AccountTypeCode` to its display label, accepting the enum case-insensitively and the
 * legacy ISO-20022 cash-account codes (`CACC`, `SVGS`, `CCRD`) so it resolves whether the bank
 * populates `AccountTypeCode` or a `CurrentAccount`-style value.
 */
private fun accountTypeLabelRes(accountTypeCode: String): StringResource = when (accountTypeCode.lowercase()) {
    "cacc", "current", "currentaccount" -> Res.string.core_ui_account_type_current
    "svgs", "savings" -> Res.string.core_ui_account_type_savings
    "card", "ccrd", "credit", "creditcard" -> Res.string.core_ui_account_type_credit
    "globalmoney" -> Res.string.core_ui_account_type_global_money
    "globalwallet" -> Res.string.core_ui_account_type_global_wallet
    else -> Res.string.core_ui_account_type_other
}
