/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.network.model.ais.balances.BalancesResponse

private const val TYPE_AVAILABLE = "Available"
private const val TYPE_BOOKED = "Booked"
private const val ZERO_AMOUNT = "0"

/**
 * Collapses the OBIE `OBReadBalance1` list into the two figures the hero card shows.
 *
 * The current balance prefers the `InterimBooked` row (falling back to the first row); the available
 * balance prefers `InterimAvailable` (falling back to the current figure) so the card always has both.
 */
fun BalancesResponse.toAccountBalance(accountId: String): AccountBalance {
    val balances = data?.balance.orEmpty()
    val booked = balances.firstOrNull { it.type?.contains(TYPE_BOOKED, ignoreCase = true) == true }
    val available = balances.firstOrNull { it.type?.contains(TYPE_AVAILABLE, ignoreCase = true) == true }
    val current = booked ?: balances.firstOrNull()
    return AccountBalance(
        accountId = accountId,
        currency = current?.amount?.currency ?: available?.amount?.currency ?: "",
        currentAmount = current?.amount?.amount ?: ZERO_AMOUNT,
        availableAmount = (available ?: current)?.amount?.amount ?: ZERO_AMOUNT,
    )
}
