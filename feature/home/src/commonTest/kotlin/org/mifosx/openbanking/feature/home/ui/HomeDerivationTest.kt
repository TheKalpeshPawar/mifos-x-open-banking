/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.home.ui

import org.mifosx.openbanking.core.model.obp.Account
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeDerivationTest {

    @Test
    fun primaryAccount_prefersCheckingType() {
        val accounts = listOf(
            Account(id = "s1", accountType = "SAVINGS"),
            Account(id = "c1", accountType = "CHECKING"),
            Account(id = "c2", accountType = "CHECKING"),
        )
        assertEquals("c1", accounts.primaryAccount()?.id)
    }

    @Test
    fun primaryAccount_fallsBackToFirstWhenNoChecking() {
        val accounts = listOf(
            Account(id = "s1", accountType = "SAVINGS"),
            Account(id = "b1", accountType = "BUSINESS"),
        )
        assertEquals("s1", accounts.primaryAccount()?.id)
    }

    @Test
    fun primaryAccount_matchesProductCodeWhenAccountTypeBlank() {
        // v7 detail responses carry product_code instead of account_type.
        val accounts = listOf(
            Account(id = "x1", productCode = "PERSONAL_CHECKING"),
        )
        assertEquals("x1", accounts.primaryAccount()?.id)
    }

    @Test
    fun primaryAccount_nullWhenEmpty() {
        assertNull(emptyList<Account>().primaryAccount())
    }

    @Test
    fun timeOfDayGreeting_boundaries() {
        assertEquals("Good morning", timeOfDayGreeting(0))
        assertEquals("Good morning", timeOfDayGreeting(11))
        assertEquals("Good afternoon", timeOfDayGreeting(12))
        assertEquals("Good afternoon", timeOfDayGreeting(16))
        assertEquals("Good evening", timeOfDayGreeting(17))
        assertEquals("Good evening", timeOfDayGreeting(23))
    }

    @Test
    fun formatDashboardDate_rendersWeekdayMonthName() {
        // 2026-05-28 is a Thursday (isoWeekday 4).
        assertEquals("Thursday, 28 May 2026", formatDashboardDate(4, 28, 5, 2026))
    }

    @Test
    fun formatMoney_groupsAndPadsDecimals() {
        assertEquals("GHS 4,250.00", formatMoney("4250.00", "GHS"))
        assertEquals("GHS 11,890.50", formatMoney("11890.5", "GHS"))
        assertEquals("EUR 109,846.37", formatMoney("109846.379999", "EUR"))
        assertEquals("GHS 0.00", formatMoney("", "GHS"))
    }

    @Test
    fun formatMoney_signedForTransactions() {
        assertEquals("+GHS 3,200.00", formatMoney("3200.00", "GHS", signed = true))
        assertEquals("-GHS 42.50", formatMoney("-42.50", "GHS", signed = true))
    }

    @Test
    fun formatMoney_unsignedNegativeBalance() {
        assertEquals("EUR -3,863.29", formatMoney("-3863.29", "EUR"))
    }

    @Test
    fun initials_takesUpToTwo() {
        assertEquals("AO", initials("Alex Owusu"))
        assertEquals("A", initials("Alex"))
        assertEquals("", initials("   "))
    }
}
