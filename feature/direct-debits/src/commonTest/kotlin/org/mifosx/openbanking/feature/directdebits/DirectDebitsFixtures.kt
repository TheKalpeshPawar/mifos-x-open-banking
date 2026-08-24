/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits

import org.mifosx.openbanking.core.model.banking.DirectDebitItem
import org.mifosx.openbanking.core.model.banking.DirectDebitsSummary
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitRowUi
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsState
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsUiState

/**
 * The direct-debits demo fixture, mirroring `idea-layer/screens/direct-debits/demo-data.yaml`:
 * three active mandates and one inactive, already in the sorted order the mapper produces.
 *
 * Shared by the view-model and Compose suites so both assert against the same four mandates the
 * design was drawn against.
 */
object DirectDebitsFixtures {

    const val ACCOUNT_ID: String = "40051512345678"

    val britishGas: DirectDebitItem = DirectDebitItem(
        mandateId = "DD-BG-44120",
        name = "British Gas",
        statusCode = "Active",
        isActive = true,
        previousPaymentAmount = "78.00",
        currency = "GBP",
        previousPaymentDateTime = "2026-06-15T00:00:00Z",
    )

    val vodafone: DirectDebitItem = DirectDebitItem(
        mandateId = "DD-VF-88301",
        name = "Vodafone",
        statusCode = "Active",
        isActive = true,
        previousPaymentAmount = "29.00",
        currency = "GBP",
        previousPaymentDateTime = "2026-06-20T00:00:00Z",
    )

    val aviva: DirectDebitItem = DirectDebitItem(
        mandateId = "DD-AV-10293",
        name = "Aviva Insurance",
        statusCode = "Active",
        isActive = true,
        previousPaymentAmount = "41.50",
        currency = "GBP",
        previousPaymentDateTime = "2026-06-05T00:00:00Z",
    )

    val tvLicensing: DirectDebitItem = DirectDebitItem(
        mandateId = "DD-TVL-55667",
        name = "TV Licensing",
        statusCode = "Inactive",
        isActive = false,
        previousPaymentAmount = "13.25",
        currency = "GBP",
        previousPaymentDateTime = "2026-03-01T00:00:00Z",
    )

    fun summary(): DirectDebitsSummary = DirectDebitsSummary(
        items = listOf(britishGas, vodafone, aviva, tvLicensing),
        activeCount = 3,
        inactiveCount = 1,
    )

    fun emptySummary(): DirectDebitsSummary = DirectDebitsSummary(
        items = emptyList(),
        activeCount = 0,
        inactiveCount = 0,
    )

    /** The display-ready rows the view model derives from [summary], for the Compose suites. */
    fun rows(): List<DirectDebitRowUi> = listOf(
        DirectDebitRowUi(
            mandateId = "DD-BG-44120",
            name = "British Gas",
            statusLabel = "Active",
            isActive = true,
            previousPaymentAmount = "£78.00",
            previousPaymentDateTime = "2026-06-15 00:00:00",
        ),
        DirectDebitRowUi(
            mandateId = "DD-VF-88301",
            name = "Vodafone",
            statusLabel = "Active",
            isActive = true,
            previousPaymentAmount = "£29.00",
            previousPaymentDateTime = "2026-06-20 00:00:00",
        ),
        DirectDebitRowUi(
            mandateId = "DD-AV-10293",
            name = "Aviva Insurance",
            statusLabel = "Active",
            isActive = true,
            previousPaymentAmount = "£41.50",
            previousPaymentDateTime = "2026-06-05 00:00:00",
        ),
        DirectDebitRowUi(
            mandateId = "DD-TVL-55667",
            name = "TV Licensing",
            statusLabel = "Inactive",
            isActive = false,
            previousPaymentAmount = "£13.25",
            previousPaymentDateTime = "2026-03-01 00:00:00",
        ),
    )

    fun contentState(): DirectDebitsState = DirectDebitsState(
        accountId = ACCOUNT_ID,
        uiState = DirectDebitsUiState.Content(
            mandates = rows(),
        ),
    )
}
