/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormatMoneyTest {

    @Test
    fun parsesDecimalIntoMinorUnits() {
        assertEquals(284763, parseMinorUnits("2847.63"))
        assertEquals(123400, parseMinorUnits("1234"))
        assertEquals(1250, parseMinorUnits("12.5"))
        assertEquals(1299, parseMinorUnits("12.999"))
        assertEquals(-840, parseMinorUnits("-8.40"))
    }

    @Test
    fun returnsNullForMalformedAmounts() {
        assertNull(parseMinorUnits(""))
        assertNull(parseMinorUnits("abc"))
        assertNull(parseMinorUnits("1.2.3"))
    }

    @Test
    fun formatsMinorUnitsWithSymbolAndGrouping() {
        assertEquals("£2,847.63", formatMinorUnits(284763, "GBP"))
        assertEquals("£0.05", formatMinorUnits(5, "GBP"))
        assertEquals("-£12.00", formatMinorUnits(-1200, "GBP"))
        assertEquals("€1,000,000.00", formatMinorUnits(100000000, "EUR"))
    }

    @Test
    fun formatsMoneyFromDecimalString() {
        assertEquals("£31.99", formatMoney("31.99", "GBP"))
    }

    @Test
    fun formatsSignedMoneyForCreditsAndDebits() {
        assertEquals("+ £2,400.00", formatSignedMoney("2400.00", "GBP", isCredit = true))
        assertEquals("- £31.99", formatSignedMoney("31.99", "GBP", isCredit = false))
    }

    @Test
    fun fallsBackToCurrencyCodeForUnknownSymbol() {
        assertEquals("PLN 5.00", formatMinorUnits(500, "PLN"))
    }
}
