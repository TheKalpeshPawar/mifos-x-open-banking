/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home

import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.feature.home.ui.Greeting
import org.mifosx.openbanking.feature.home.ui.HomeData

/**
 * Accounts shaped like the HSBC sandbox: a current account, a savings account, a Global Money wallet
 * that reports `CACC` and is only identifiable by its description, and a card.
 */
object HomeFixtures {

    const val CURRENT_ID = "acc-current"
    const val SAVINGS_ID = "acc-savings"
    const val GLOBAL_MONEY_ID = "acc-global-money"
    const val CARD_ID = "acc-card"

    fun current(): AccountWithBalance = AccountWithBalance(
        account = BankAccount(
            accountId = CURRENT_ID,
            accountTypeCode = "CACC",
            currency = "GBP",
            identification = "80200110203349",
            scheme = AccountScheme.SortCode,
            description = "Description of the account",
            accountHolderName = "Mr Nico",
        ),
        balance = AccountBalance(CURRENT_ID, "GBP", "2900.00", "2847.63"),
    )

    fun savings(): AccountWithBalance = AccountWithBalance(
        account = BankAccount(
            accountId = SAVINGS_ID,
            accountTypeCode = "SVGS",
            currency = "GBP",
            identification = "80122590953695",
            scheme = AccountScheme.SortCode,
            description = "BMM ACCOUNT",
            accountHolderName = "ACCNT SHORT",
        ),
        balance = AccountBalance(SAVINGS_ID, "GBP", "512.00", "512.00"),
    )

    /** Reports `CACC` like a current account; only the description marks it as a wallet. */
    fun globalMoney(): AccountWithBalance = AccountWithBalance(
        account = BankAccount(
            accountId = GLOBAL_MONEY_ID,
            accountTypeCode = "CACC",
            currency = "GBP",
            identification = "80119770009652",
            scheme = AccountScheme.SortCode,
            description = "GLOBAL MONEY ACCOUNT",
            accountHolderName = "Global Money GMA",
        ),
        balance = AccountBalance(GLOBAL_MONEY_ID, "GBP", "120.00", "120.00"),
    )

    /** The bank returns an already-masked PAN and no description. */
    fun card(): AccountWithBalance = AccountWithBalance(
        account = BankAccount(
            accountId = CARD_ID,
            accountTypeCode = "CARD",
            currency = "GBP",
            identification = "xxxx-xxxx-xxxx-3456",
            scheme = AccountScheme.Pan,
            accountHolderName = "Mr Bantu",
        ),
        balance = AccountBalance(CARD_ID, "GBP", "342.18", "342.18"),
    )

    fun all(): List<AccountWithBalance> = listOf(current(), savings(), globalMoney(), card())

    fun homeData(
        accounts: List<AccountWithBalance> = listOf(current(), savings(), globalMoney()),
        cards: List<AccountWithBalance> = listOf(card()),
        greeting: Greeting = Greeting.Morning,
    ): HomeData = HomeData(accounts = accounts, cards = cards, greeting = greeting)
}
