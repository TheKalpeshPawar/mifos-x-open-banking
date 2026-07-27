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

/**
 * The Explore chip row's destinations, in the order they are rendered.
 *
 * Declaration order is the display order and is asserted by the UI suites — Standing Orders
 * precedes Direct Debits, matching the design. Every chip carries the current `accountId` to its
 * destination; none of them navigate without it.
 */
enum class AccountDetailChip {
    Transactions,
    Statements,
    StandingOrders,
    DirectDebits,
    ScheduledPayments,
    Beneficiaries,
    AtmLocator,
    Product,
    Party,
}
