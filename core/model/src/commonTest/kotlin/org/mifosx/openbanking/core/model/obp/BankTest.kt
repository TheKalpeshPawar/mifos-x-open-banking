/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.obp

import kotlin.test.Test
import kotlin.test.assertEquals

class BankTest {

    @Test
    fun swiftBic_prefersBicRouting() {
        val bank = Bank(
            bankRoutings = listOf(AccountRouting(scheme = "BIC", address = "NEONDEBB")),
            attributes = listOf(BankAttribute(name = "SWIFT_BIC", value = "ACMEGB2L")),
        )
        assertEquals("NEONDEBB", bank.swiftBic)
    }

    @Test
    fun swiftBic_fallsBackToAttribute() {
        val bank = Bank(
            bankRoutings = listOf(AccountRouting(scheme = "OBP", address = "ac.bank.uk")),
            attributes = listOf(BankAttribute(name = "SWIFT_BIC", value = "ACMEGB2L")),
        )
        assertEquals("ACMEGB2L", bank.swiftBic)
    }

    @Test
    fun countryCode_isBicPositions5And6() {
        assertEquals("GB", Bank(attributes = listOf(BankAttribute("SWIFT_BIC", "ACMEGB2L"))).countryCode)
        assertEquals("DE", Bank(bankRoutings = listOf(AccountRouting("BIC", "NEONDEBB"))).countryCode)
    }

    @Test
    fun countryCode_emptyWhenNoBic() {
        assertEquals("", Bank(id = "ac.bank.uk").countryCode)
        assertEquals("", Bank(attributes = listOf(BankAttribute("SWIFT_BIC", "ABC"))).countryCode)
    }
}
