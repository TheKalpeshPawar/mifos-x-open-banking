/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail

import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.model.banking.AccountDetailWithBalances

/** Domain fixtures shared by the view-model tests; the UI suites inline their own display state. */
object AccountDetailFixtures {

    fun detail(
        accountId: String = "acc-1",
        nickname: String = "Everyday Current",
        accountSubType: String = "CurrentAccount",
        currency: String = "GBP",
        sortCode: String = "400515",
        accountNumber: String = "12345678",
        servicerIdentification: String = "MIDLGB2105V",
        statusUpdateDateTime: String = "2026-06-28T18:30:00Z",
        // Defaults describe an ordinary personal current account, so a test that does not care
        // about capabilities gets every chip without saying so.
        accountTypeCode: String = "CACC",
        description: String = "Description of the account",
    ): AccountDetail = AccountDetail(
        accountId = accountId,
        nickname = nickname,
        accountSubType = accountSubType,
        currency = currency,
        sortCode = sortCode,
        accountNumber = accountNumber,
        servicerIdentification = servicerIdentification,
        statusUpdateDateTime = statusUpdateDateTime,
        accountTypeCode = accountTypeCode,
        description = description,
    )

    fun balances(): List<AccountBalanceLine> = listOf(
        AccountBalanceLine(
            type = "InterimAvailable",
            amount = "2847.63",
            currency = "GBP",
            dateTime = "2026-06-28T18:30:00Z",
        ),
        AccountBalanceLine(
            type = "InterimBooked",
            amount = "2905.10",
            currency = "GBP",
            dateTime = "2026-06-28T18:30:00Z",
        ),
    )

    fun withBalances(
        detail: AccountDetail = detail(),
        balances: List<AccountBalanceLine> = balances(),
    ): AccountDetailWithBalances = AccountDetailWithBalances(detail = detail, balances = balances)
}
