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
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_credit
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_current
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_global_money
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_global_wallet
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_other
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_savings

/** The localized account-type label on its own, e.g. `Current Account`. */
@Composable
fun accountTypeLabel(accountTypeCode: String, description: String): String =
    stringResource(accountTypeLabelRes(accountTypeCode, description))

/** The localized label for an already-resolved product. */
@Composable
fun accountTypeLabel(product: HsbcProductType): String =
    stringResource(product.labelRes())

/**
 * The single line to title an account with: the holder name (OBIE nested `Account[].Name`) when the
 * bank supplies one, otherwise the account-type label.
 */
@Composable
fun accountTitle(
    accountHolderName: String,
    accountTypeCode: String,
    description: String,
): String = accountHolderName.ifBlank { accountTypeLabel(accountTypeCode, description) }

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
