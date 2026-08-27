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
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
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
private val CREDIT_CARD_TYPE_CODES = setOf("card", "ccrd")

/** The localized account-type label on its own, e.g. `Current Account`. */
@Composable
fun accountTypeLabel(accountTypeCode: String, description: String): String =
    stringResource(accountTypeLabelRes(accountTypeCode, description))

/** The localized label for an already-resolved product. */
@Composable
fun accountTypeLabel(product: HsbcProductType): String =
    stringResource(product.labelRes())

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
    description: String,
    scheme: AccountScheme,
    identification: String,
): String {
    if (accountHolderName.isNotBlank()) return accountHolderName
    val typeLabel = stringResource(accountTypeLabelRes(accountTypeCode, description))
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

/** This product's display label. */
internal fun HsbcProductType.labelRes(): StringResource = when (this) {
    HsbcProductType.PersonalCurrentAccount -> Res.string.core_ui_account_type_current
    HsbcProductType.Savings -> Res.string.core_ui_account_type_savings
    HsbcProductType.CreditCard -> Res.string.core_ui_account_type_credit
    HsbcProductType.ForeignCurrency -> Res.string.core_ui_account_type_global_wallet
    HsbcProductType.GlobalMoney -> Res.string.core_ui_account_type_global_money
    HsbcProductType.Unknown -> Res.string.core_ui_account_type_other
}

/**
 * The display label for the product an OBIE `AccountTypeCode` and `Description` identify.
 *
 * The description is required because a Global Money wallet reports `CACC`, the same code as a current
 * account, and only [HsbcProductType.resolve] tells them apart.
 */
internal fun accountTypeLabelRes(accountTypeCode: String, description: String): StringResource =
    HsbcProductType.resolve(accountTypeCode = accountTypeCode, description = description).labelRes()
