/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.core.model.banking.TransactionDetail
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailErrorKind
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailState
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailUiModel
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailUiState
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError

/**
 * The transaction-detail states the suites render, carrying the HSBC-sandbox demo set the design was
 * drawn against (accountId `40051512345678`): a booked debit at Tesco, a salary credit with no
 * merchant, and a pending Pret pre-auth.
 *
 * Shared by the view-model, Compose, Robolectric and screenshot suites so all assert against one
 * screen rather than several that happen to look alike.
 */
object TransactionDetailFixtures {

    const val ACCOUNT_ID: String = "40051512345678"

    const val DEBIT_ID: String = "TX-20260626-0001"
    const val CREDIT_ID: String = "TX-20260625-0001"
    const val PENDING_ID: String = "TX-20260629-0001"

    /** A booked card debit at Tesco: MCC 5411 → Groceries, running balance in credit. */
    fun debit(): TransactionDetail = TransactionDetail(
        transactionId = DEBIT_ID,
        accountId = ACCOUNT_ID,
        amount = "42.17",
        currency = "GBP",
        isCredit = false,
        merchantName = "Tesco Stores",
        status = "Booked",
        bookingDateTime = "2026-06-26T11:22:00Z",
        valueDateTime = "2026-06-26T11:22:00Z",
        category = TransactionCategory.GROCERIES,
        merchantCategoryCode = "5411",
        balanceAmount = "447.63",
        balanceCurrency = "GBP",
        balanceIsCredit = true,
        transactionInformation = "TESCO STORES 3476 LONDON",
        proprietaryCode = "DR",
        proprietaryIssuer = "HSBC",
    )

    /**
     * A salary credit with no merchant details: `merchantName` is null so the merchant line falls back
     * to the narrative, and `merchantCategoryCode` is null so the MCC row hides. Category resolves to
     * OTHER (no MCC, non-transfer proprietary code).
     */
    fun credit(): TransactionDetail = TransactionDetail(
        transactionId = CREDIT_ID,
        accountId = ACCOUNT_ID,
        amount = "2400.00",
        currency = "GBP",
        isCredit = true,
        merchantName = null,
        status = "Booked",
        bookingDateTime = "2026-06-25T08:00:00Z",
        valueDateTime = "2026-06-25T00:00:00Z",
        category = TransactionCategory.OTHER,
        merchantCategoryCode = null,
        balanceAmount = "2847.63",
        balanceCurrency = "GBP",
        balanceIsCredit = true,
        transactionInformation = "SALARY JUN ACME LTD",
        proprietaryCode = "CR",
        proprietaryIssuer = "HSBC",
    )

    /** A pending Pret pre-auth: Status Pending drives the secondary-container chip; MCC 5812 → Dining. */
    fun pending(): TransactionDetail = TransactionDetail(
        transactionId = PENDING_ID,
        accountId = ACCOUNT_ID,
        amount = "6.45",
        currency = "GBP",
        isCredit = false,
        merchantName = "Pret A Manger",
        status = "Pending",
        bookingDateTime = "2026-06-29T09:15:00Z",
        valueDateTime = "2026-06-30T00:00:00Z",
        category = TransactionCategory.DINING,
        merchantCategoryCode = "5812",
        balanceAmount = "441.18",
        balanceCurrency = "GBP",
        balanceIsCredit = true,
        transactionInformation = "PRET A MANGER LONDON",
        proprietaryCode = "DR",
        proprietaryIssuer = "HSBC",
    )

    /** The full account list the stream hands the view model, which resolves one by `transactionId`. */
    fun transactionList(): List<TransactionDetail> = listOf(debit(), credit(), pending())

    fun contentStreamState(): ScreenState<List<TransactionDetail>> =
        ScreenState.Content(data = transactionList(), freshness = DataFreshness.FRESH)

    fun emptyStreamState(): ScreenState<List<TransactionDetail>> =
        ScreenState.Content(data = emptyList(), freshness = DataFreshness.FRESH)

    fun errorStreamState(error: NetworkError): ScreenState<List<TransactionDetail>> =
        ScreenState.Error(RemoteException(error))

    // ── Rendered UI states for the Compose suites ────────────────────────────────

    /** The debit as the view model formats it — every string finished, MCC present. */
    fun debitUiModel(): TransactionDetailUiModel = TransactionDetailUiModel(
        amountLabel = "-£42.17",
        isCredit = false,
        currencyLabel = "GBP",
        merchantLabel = "Tesco Stores",
        status = "Booked",
        isBooked = true,
        bookingDateLabel = "26 Jun 2026, 11:22",
        valueDateLabel = "26 Jun 2026, 11:22",
        category = TransactionCategory.GROCERIES,
        merchantCategoryCode = "5411",
        balanceAfterLabel = "£447.63",
        referenceLabel = "TESCO STORES 3476 LONDON",
        bankCodeLabel = "DR · HSBC",
    )

    /** The credit as the view model formats it — merchant fallback, MCC null (row hidden). */
    fun creditUiModel(): TransactionDetailUiModel = TransactionDetailUiModel(
        amountLabel = "+£2,400.00",
        isCredit = true,
        currencyLabel = "GBP",
        merchantLabel = "SALARY JUN ACME LTD",
        status = "Booked",
        isBooked = true,
        bookingDateLabel = "25 Jun 2026, 08:00",
        valueDateLabel = "25 Jun 2026, 00:00",
        category = TransactionCategory.OTHER,
        merchantCategoryCode = null,
        balanceAfterLabel = "£2,847.63",
        referenceLabel = "SALARY JUN ACME LTD",
        bankCodeLabel = "CR · HSBC",
    )

    fun contentState(model: TransactionDetailUiModel = debitUiModel()): TransactionDetailState =
        TransactionDetailState(
            transactionId = DEBIT_ID,
            accountId = ACCOUNT_ID,
            uiState = TransactionDetailUiState.Content(model),
        )

    fun loadingState(): TransactionDetailState =
        TransactionDetailState(transactionId = DEBIT_ID, accountId = ACCOUNT_ID)

    fun emptyState(): TransactionDetailState = TransactionDetailState(
        transactionId = DEBIT_ID,
        accountId = ACCOUNT_ID,
        uiState = TransactionDetailUiState.Empty,
    )

    fun errorState(
        kind: TransactionDetailErrorKind = TransactionDetailErrorKind.TokenExpired,
    ): TransactionDetailState = TransactionDetailState(
        transactionId = DEBIT_ID,
        accountId = ACCOUNT_ID,
        uiState = TransactionDetailUiState.Error(kind),
    )

    /** Content carrying a null MCC (the salary credit) — the MCC row hides. */
    fun mccNullContentState(): TransactionDetailState = contentState(creditUiModel())

    /** A recoverable failure (expired token) — the error state offers Retry. */
    fun recoverableErrorState(): TransactionDetailState =
        errorState(TransactionDetailErrorKind.TokenExpired)

    /** A non-recoverable failure (withdrawn consent) — the error state offers Go Back. */
    fun nonRecoverableErrorState(): TransactionDetailState =
        errorState(TransactionDetailErrorKind.ConsentWithdrawn)
}
