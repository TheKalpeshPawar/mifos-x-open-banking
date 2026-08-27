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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.common.formatAccountIdentifier
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
import template.core.base.designsystem.theme.KptTheme

/**
 * An account as three stacked lines: product type, account holder name, then identification.
 *
 * The holder line is omitted when the bank supplies no name.
 *
 * @param product The resolved product, whose localized label is the first line.
 * @param accountHolderName OBIE nested `Account[].Name`.
 * @param scheme The identification's scheme, which decides how it is grouped.
 * @param identification The bank's account identifier, rendered as sent.
 */
@Composable
fun AccountIdentityLines(
    product: HsbcProductType,
    accountHolderName: String,
    scheme: AccountScheme,
    identification: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = accountTypeLabel(product),
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )

        if (accountHolderName.isNotBlank()) {
            Text(
                text = accountHolderName,
                style = KptTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = KptTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        Text(
            text = formatAccountIdentifier(scheme, identification),
            style = KptTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * An account as three stacked lines, resolving the product from its OBIE fields.
 *
 * @param accountTypeCode OBIE `AccountTypeCode`, e.g. `CACC`.
 * @param description OBIE `Description`, e.g. `GLOBAL MONEY ACCOUNT`.
 */
@Composable
fun AccountIdentityLines(
    accountTypeCode: String,
    description: String,
    accountHolderName: String,
    scheme: AccountScheme,
    identification: String,
    modifier: Modifier = Modifier,
) = AccountIdentityLines(
    product = HsbcProductType.resolve(accountTypeCode = accountTypeCode, description = description),
    accountHolderName = accountHolderName,
    scheme = scheme,
    identification = identification,
    modifier = modifier,
)

/**
 * A party as two stacked lines: name, then identification.
 *
 * For an account whose product is unknown — a payee, or a payer read back off a payment response,
 * neither of which carries an account type.
 *
 * @param name The account holder's name. The line is omitted when blank.
 * @param scheme The identification's scheme, which decides how it is grouped.
 * @param identification The bank's account identifier, rendered as sent.
 * @param horizontalAlignment How the two lines sit against each other.
 */
@Composable
fun AccountIdentityLines(
    name: String,
    scheme: AccountScheme,
    identification: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        horizontalAlignment = horizontalAlignment,
    ) {
        if (name.isNotBlank()) {
            Text(
                text = name,
                style = KptTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = KptTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        Text(
            text = formatAccountIdentifier(scheme, identification),
            style = KptTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A labelled party block for a review screen: the field label, then the party's name over its
 * identification.
 *
 * For a party with no account type of its own — a payee.
 */
@Composable
fun MifosPartyReviewRow(
    label: String,
    name: String,
    scheme: AccountScheme,
    identification: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = label,
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        AccountIdentityLines(name = name, scheme = scheme, identification = identification)
    }
}

/**
 * A labelled account block for a review screen: the field label, then the account as three lines, or
 * [absentLabel] when the customer chose no account.
 *
 * @param label The field label, e.g. "From".
 * @param account The chosen account, or null when the bank will choose.
 * @param absentLabel What to show in place of the account when [account] is null.
 */
@Composable
fun MifosAccountReviewRow(
    label: String,
    account: BankAccount?,
    absentLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = label,
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        if (account == null) {
            Text(
                text = absentLabel,
                style = KptTheme.typography.bodyLarge,
                color = KptTheme.colorScheme.onSurface,
            )
        } else {
            AccountIdentityLines(
                accountTypeCode = account.accountTypeCode,
                description = account.description,
                accountHolderName = account.accountHolderName,
                scheme = account.scheme,
                identification = account.identification,
            )
        }
    }
}

@Preview
@Composable
private fun AccountIdentityLinesPreview() {
    MifosXOpenBankingTheme {
        AccountIdentityLines(
            accountTypeCode = "CACC",
            description = "GLOBAL MONEY ACCOUNT",
            accountHolderName = "Global Money GMA",
            scheme = AccountScheme.SortCode,
            identification = "80200110209652",
        )
    }
}

@Preview
@Composable
private fun AccountIdentityLinesWithoutHolderNamePreview() {
    MifosXOpenBankingTheme {
        AccountIdentityLines(
            accountTypeCode = "CACC",
            description = "Description of the account",
            accountHolderName = "",
            scheme = AccountScheme.SortCode,
            identification = "80200110203349",
        )
    }
}
