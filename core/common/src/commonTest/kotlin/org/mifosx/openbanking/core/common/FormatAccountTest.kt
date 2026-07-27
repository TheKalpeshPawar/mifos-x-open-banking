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

class FormatAccountTest {

    @Test
    fun groupsSixDigitSortCode() {
        assertEquals("40-05-15", formatSortCode("400515"))
    }

    @Test
    fun returnsInputUnchangedWhenNotSixDigits() {
        assertEquals("4005", formatSortCode("4005"))
        assertEquals("", formatSortCode(""))
    }

    @Test
    fun masksAllButTheLastFourCardDigits() {
        assertEquals("•••• 7654", maskCardNumber("4111111111117654"))
    }

    @Test
    fun maskCardNumberKeepsWhateverShortInputHas() {
        assertEquals("•••• 654", maskCardNumber("654"))
        assertEquals("•••• ", maskCardNumber(""))
    }

    @Test
    fun groupsIbanIntoBlocksOfFour() {
        assertEquals("GB29 HBUK 4005 1512 3456 78", formatIban("GB29HBUK40051512345678"))
    }

    @Test
    fun returnsIbanUnchangedWhenFourOrFewerChars() {
        assertEquals("GB29", formatIban("GB29"))
        assertEquals("GB", formatIban("GB"))
    }

    @Test
    fun formatsAccountIdentifierPerSubtype() {
        assertEquals(
            "40-05-15  12345678",
            formatAccountIdentifier("CurrentAccount", "40051512345678", "400515", "12345678"),
        )
        assertEquals(
            "40-05-15  87654321",
            formatAccountIdentifier("Savings", "40051587654321", "400515", "87654321"),
        )
        assertEquals(
            "•••• 7654",
            formatAccountIdentifier("CreditCard", "4111111111117654", "", ""),
        )
        assertEquals(
            "GB29 HBUK 4005 1512 3456 78",
            formatAccountIdentifier("GlobalMoney", "GB29HBUK40051512345678", "", ""),
        )
        assertEquals(
            "GB29 HBUK 4005 1512 3456 78",
            formatAccountIdentifier("GlobalWallet", "GB29HBUK40051512345678", "", ""),
        )
        assertEquals(
            "raw-fallback",
            formatAccountIdentifier("SomethingElse", "raw-fallback", "400515", "12345678"),
        )
    }
}
