/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking.payment

import kotlinx.serialization.Serializable

/**
 * Who pays the fees on an international payment.
 *
 * International only. HSBC's own collection carries `ChargeBearer` on every international request
 * and on none of the domestic ones, and omitting it internationally is refused with `U004`. Modelled
 * as an enum rather than a raw string so a typo cannot reach the bank: the wire values are
 * capitalised exactly as OBIE spells them, and [wireValue] is the single place that spelling lives.
 *
 * Only [BorneByCreditor] is exercised by HSBC's sample requests; the other three are defined by OBIE
 * and offered to the PSU, but have not been confirmed against the sandbox.
 */
@Serializable
enum class ChargeBearer(val wireValue: String) {
    /** The recipient pays. The value HSBC's reference collection sends. */
    BorneByCreditor("BorneByCreditor"),

    /** The payer pays. */
    BorneByDebtor("BorneByDebtor"),

    /** Split between payer and recipient. */
    Shared("Shared"),

    /** Whatever the underlying scheme's service level dictates. */
    FollowingServiceLevel("FollowingServiceLevel"),
    ;

    companion object {
        /** Resolves a persisted or echoed wire value, falling back to the default HSBC demonstrates. */
        fun fromWire(raw: String?): ChargeBearer =
            entries.firstOrNull { it.wireValue == raw } ?: BorneByCreditor
    }
}
