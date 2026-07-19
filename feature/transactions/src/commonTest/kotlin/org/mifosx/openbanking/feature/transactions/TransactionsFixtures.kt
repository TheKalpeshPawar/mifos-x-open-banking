/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions

import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.feature.transactions.ui.TransactionGroup
import org.mifosx.openbanking.feature.transactions.ui.TransactionRowUi
import org.mifosx.openbanking.feature.transactions.ui.TransactionsData
import org.mifosx.openbanking.feature.transactions.ui.TransactionsState
import org.mifosx.openbanking.feature.transactions.ui.TransactionsUiState

/** Shared display fixtures for the Robolectric + instrumented screen tests. */
object TransactionsFixtures {

    private val rows = listOf(
        TransactionRowUi(
            key = "t1",
            transactionId = "t1",
            description = "TESCO STORES",
            amountLabel = "- £42.17",
            isCredit = false,
            category = TransactionCategory.GROCERIES,
            isPending = false,
        ),
        TransactionRowUi(
            key = "t2",
            transactionId = "t2",
            description = "SPOTIFY AB",
            amountLabel = "- £11.99",
            isCredit = false,
            category = TransactionCategory.SUBSCRIPTIONS,
            isPending = true,
        ),
    )

    fun contentState(
        hasNextPage: Boolean = false,
        isPaginating: Boolean = false,
    ): TransactionsState = TransactionsState(
        accountId = "acc-1",
        uiState = TransactionsUiState.Content(
            TransactionsData(
                groups = listOf(TransactionGroup(dateLabel = "SAT 27 JUN 2026", rows = rows)),
                moneyInLabel = "+£0.00",
                moneyOutLabel = "-£54.16",
            ),
        ),
        hasNextPage = hasNextPage,
        isPaginating = isPaginating,
    )
}
