/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking

/**
 * Pairs a [BankAccount] with its [AccountBalance] for the accounts overview screen.
 *
 * [balance] is nullable on purpose: balances are fetched per account, so one account's balance
 * fetch failing must leave the account visible with a missing figure rather than fail the whole
 * screen. A null balance means "the balance could not be resolved", distinct from a zero balance.
 *
 * @property account The consented account.
 * @property balance The account's resolved balance, or null when its fetch failed.
 */
data class AccountWithBalance(
    val account: BankAccount,
    val balance: AccountBalance?,
)
