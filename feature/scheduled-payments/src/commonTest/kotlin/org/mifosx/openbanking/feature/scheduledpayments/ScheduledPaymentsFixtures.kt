/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.scheduledpayments

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentItem
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentType
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentUiModel
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsError
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsState
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsUiState
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError

/**
 * The scheduled-payments states the suites render, carrying the HSBC-sandbox demo set the design was
 * drawn against (accountId `40051512345678`): an HMRC execution-date payment and a Direct Line
 * arrival-date payment, so both ScheduledType chips are exercised.
 *
 * Shared by the view-model, Compose, Robolectric and screenshot suites so all assert against one
 * screen rather than several that happen to look alike.
 */
object ScheduledPaymentsFixtures {

    const val ACCOUNT_ID: String = "40051512345678"

    const val EXECUTION_ID: String = "SP-001"
    const val ARRIVAL_ID: String = "SP-003"

    /** An execution-date payment: money leaves the account on 31 Jul 2026. */
    fun executionPayment(): ScheduledPaymentItem = ScheduledPaymentItem(
        scheduledPaymentId = EXECUTION_ID,
        accountId = ACCOUNT_ID,
        payeeName = "HMRC Self Assessment",
        amount = "842.00",
        currency = "GBP",
        scheduledDateTime = "2026-07-31T00:00:00Z",
        scheduledType = ScheduledPaymentType.Execution,
        reference = "HMRC-SA-2526",
        creditorIdentification = "08-32-00 12001039",
    )

    /** An arrival-date payment: funds reach the beneficiary on 15 Aug 2026. */
    fun arrivalPayment(): ScheduledPaymentItem = ScheduledPaymentItem(
        scheduledPaymentId = ARRIVAL_ID,
        accountId = ACCOUNT_ID,
        payeeName = "Direct Line Insurance",
        amount = "412.50",
        currency = "GBP",
        scheduledDateTime = "2026-08-15T00:00:00Z",
        scheduledType = ScheduledPaymentType.Arrival,
        reference = "DL-HOME-INS-26",
        creditorIdentification = "20-00-00 73428901",
    )

    /** The full list the stream hands the view model. */
    fun paymentList(): List<ScheduledPaymentItem> = listOf(executionPayment(), arrivalPayment())

    fun contentStreamState(): ScreenState<List<ScheduledPaymentItem>> =
        ScreenState.Content(data = paymentList(), freshness = DataFreshness.FRESH)

    fun emptyStreamState(): ScreenState<List<ScheduledPaymentItem>> =
        ScreenState.Content(data = emptyList(), freshness = DataFreshness.FRESH)

    fun errorStreamState(error: NetworkError): ScreenState<List<ScheduledPaymentItem>> =
        ScreenState.Error(RemoteException(error))

    // ── Rendered UI states for the Compose suites ────────────────────────────────

    /** The execution payment as the view model formats it — `GBP 842.00`, `Fri 31 Jul 2026`. */
    fun executionUiModel(): ScheduledPaymentUiModel = ScheduledPaymentUiModel(
        scheduledPaymentId = EXECUTION_ID,
        payeeName = "HMRC Self Assessment",
        amountLabel = "GBP 842.00",
        scheduledDateLabel = "Fri 31 Jul 2026",
        scheduledType = ScheduledPaymentType.Execution,
        creditorIdentification = "08-32-00 12001039",
        reference = "HMRC-SA-2526",
    )

    /** The arrival payment as the view model formats it — `GBP 412.50`, `Sat 15 Aug 2026`. */
    fun arrivalUiModel(): ScheduledPaymentUiModel = ScheduledPaymentUiModel(
        scheduledPaymentId = ARRIVAL_ID,
        payeeName = "Direct Line Insurance",
        amountLabel = "GBP 412.50",
        scheduledDateLabel = "Sat 15 Aug 2026",
        scheduledType = ScheduledPaymentType.Arrival,
        creditorIdentification = "20-00-00 73428901",
        reference = "DL-HOME-INS-26",
    )

    fun contentState(): ScheduledPaymentsState = ScheduledPaymentsState(
        accountId = ACCOUNT_ID,
        uiState = ScheduledPaymentsUiState.Content(listOf(executionUiModel(), arrivalUiModel())),
    )

    fun loadingState(): ScheduledPaymentsState = ScheduledPaymentsState(accountId = ACCOUNT_ID)

    fun emptyState(): ScheduledPaymentsState = ScheduledPaymentsState(
        accountId = ACCOUNT_ID,
        uiState = ScheduledPaymentsUiState.Empty,
    )

    fun errorState(
        kind: ScheduledPaymentsError = ScheduledPaymentsError.TokenExpired,
    ): ScheduledPaymentsState = ScheduledPaymentsState(
        accountId = ACCOUNT_ID,
        uiState = ScheduledPaymentsUiState.Error(kind),
    )
}
