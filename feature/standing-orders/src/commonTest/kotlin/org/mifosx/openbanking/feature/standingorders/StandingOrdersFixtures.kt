/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders

import org.mifosx.openbanking.core.model.banking.StandingOrderItem
import org.mifosx.openbanking.core.model.banking.StandingOrdersSummary
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrderRowUi
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersState
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersUiState

/**
 * The standing-orders demo fixture, mirroring `idea-layer/screens/standing-orders/demo-data.yaml`:
 * four active orders and one inactive, already in the sorted order the mapper produces.
 *
 * Shared by the view-model and Compose suites so both assert against the same five orders the
 * design was drawn against.
 */
object StandingOrdersFixtures {

    const val ACCOUNT_ID: String = "40051512345678"

    val jamesonLettings: StandingOrderItem = StandingOrderItem(
        standingOrderId = "SO-001",
        payeeName = "Jameson Lettings",
        statusCode = "Active",
        isActive = true,
        nextPaymentAmount = "1200.00",
        currency = "GBP",
        frequencyLabel = "Monthly on the 1st",
        nextPaymentDateTime = "2026-07-01T00:00:00Z",
        finalPaymentDateTime = "",
        hasFinalPayment = false,
        creditorIdentification = "40-12-09 65872310",
        reference = "RENT-FLAT12",
    )

    val isaSaver: StandingOrderItem = StandingOrderItem(
        standingOrderId = "SO-002",
        payeeName = "ISA Saver",
        statusCode = "Active",
        isActive = true,
        nextPaymentAmount = "200.00",
        currency = "GBP",
        frequencyLabel = "Monthly on the 1st",
        nextPaymentDateTime = "2026-07-01T00:00:00Z",
        finalPaymentDateTime = "",
        hasFinalPayment = false,
        creditorIdentification = "60-16-13 31926819",
        reference = "ISA-TOPUP",
    )

    val pureGym: StandingOrderItem = StandingOrderItem(
        standingOrderId = "SO-003",
        payeeName = "PureGym",
        statusCode = "Active",
        isActive = true,
        nextPaymentAmount = "24.99",
        currency = "GBP",
        frequencyLabel = "Monthly on the 15th",
        nextPaymentDateTime = "2026-07-15T00:00:00Z",
        finalPaymentDateTime = "",
        hasFinalPayment = false,
        creditorIdentification = "20-00-00 55512345",
        reference = "GYM-MBR",
    )

    val marcusSavings: StandingOrderItem = StandingOrderItem(
        standingOrderId = "SO-005",
        payeeName = "Marcus Savings",
        statusCode = "Active",
        isActive = true,
        nextPaymentAmount = "50.00",
        currency = "GBP",
        frequencyLabel = "Weekly every Friday",
        nextPaymentDateTime = "2026-07-04T00:00:00Z",
        finalPaymentDateTime = "2026-12-25T00:00:00Z",
        hasFinalPayment = true,
        creditorIdentification = "30-96-22 41227714",
        reference = "SAVINGS-SWEEP",
    )

    /** Cancelled, so the bank sends no next payment but does send the final one it already made. */
    val oxfam: StandingOrderItem = StandingOrderItem(
        standingOrderId = "SO-004",
        payeeName = "Oxfam GB",
        statusCode = "Inactive",
        isActive = false,
        nextPaymentAmount = "10.00",
        currency = "GBP",
        frequencyLabel = "Monthly on the 28th",
        nextPaymentDateTime = "",
        finalPaymentDateTime = "2025-12-28T00:00:00Z",
        hasFinalPayment = true,
        creditorIdentification = "08-60-01 20321982",
        reference = "CHARITY-DON",
    )

    fun summary(): StandingOrdersSummary = StandingOrdersSummary(
        items = listOf(jamesonLettings, isaSaver, pureGym, marcusSavings, oxfam),
        activeCount = 4,
        inactiveCount = 1,
    )

    fun emptySummary(): StandingOrdersSummary = StandingOrdersSummary(
        items = emptyList(),
        activeCount = 0,
        inactiveCount = 0,
    )

    /** The display-ready rows the view model derives from [summary], for the Compose suites. */
    fun rows(): List<StandingOrderRowUi> = listOf(
        StandingOrderRowUi(
            standingOrderId = "SO-001",
            payeeName = "Jameson Lettings",
            statusLabel = "Active",
            isActive = true,
            amountLabel = "£1,200.00",
            currencyLabel = "GBP",
            frequencyLabel = "Monthly on the 1st",
            nextDateLabel = "1 Jul 2026",
            finalDateLabel = "",
            hasFinalPayment = false,
            sortCodeLabel = "40-12-09 65872310",
            referenceLabel = "RENT-FLAT12",
        ),
        StandingOrderRowUi(
            standingOrderId = "SO-002",
            payeeName = "ISA Saver",
            statusLabel = "Active",
            isActive = true,
            amountLabel = "£200.00",
            currencyLabel = "GBP",
            frequencyLabel = "Monthly on the 1st",
            nextDateLabel = "1 Jul 2026",
            finalDateLabel = "",
            hasFinalPayment = false,
            sortCodeLabel = "60-16-13 31926819",
            referenceLabel = "ISA-TOPUP",
        ),
        StandingOrderRowUi(
            standingOrderId = "SO-003",
            payeeName = "PureGym",
            statusLabel = "Active",
            isActive = true,
            amountLabel = "£24.99",
            currencyLabel = "GBP",
            frequencyLabel = "Monthly on the 15th",
            nextDateLabel = "15 Jul 2026",
            finalDateLabel = "",
            hasFinalPayment = false,
            sortCodeLabel = "20-00-00 55512345",
            referenceLabel = "GYM-MBR",
        ),
        StandingOrderRowUi(
            standingOrderId = "SO-005",
            payeeName = "Marcus Savings",
            statusLabel = "Active",
            isActive = true,
            amountLabel = "£50.00",
            currencyLabel = "GBP",
            frequencyLabel = "Weekly every Friday",
            nextDateLabel = "4 Jul 2026",
            finalDateLabel = "25 Dec 2026",
            hasFinalPayment = true,
            sortCodeLabel = "30-96-22 41227714",
            referenceLabel = "SAVINGS-SWEEP",
        ),
        StandingOrderRowUi(
            standingOrderId = "SO-004",
            payeeName = "Oxfam GB",
            statusLabel = "Inactive",
            isActive = false,
            amountLabel = "£10.00",
            currencyLabel = "GBP",
            frequencyLabel = "Monthly on the 28th",
            nextDateLabel = "",
            finalDateLabel = "28 Dec 2025",
            hasFinalPayment = true,
            sortCodeLabel = "08-60-01 20321982",
            referenceLabel = "CHARITY-DON",
        ),
    )

    fun contentState(): StandingOrdersState = StandingOrdersState(
        accountId = ACCOUNT_ID,
        uiState = StandingOrdersUiState.Content(
            orders = rows(),
        ),
    )
}
