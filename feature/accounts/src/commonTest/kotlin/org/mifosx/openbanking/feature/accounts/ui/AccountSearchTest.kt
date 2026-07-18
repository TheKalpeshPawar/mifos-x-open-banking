/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.accounts.ui

import org.mifosx.openbanking.core.model.obp.Account
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountSearchTest {

    private val accounts = listOf(
        Account(id = "ac.checking.001", label = "Main Checking", number = "1001", iban = "GB00MIFOS1001"),
        Account(id = "ac.savings.001", label = "Easy Savings", number = "2002", iban = "GB00MIFOS2002"),
        Account(id = "mifos.techstart.current", label = "TechStart Business", number = "3003"),
    )

    @Test
    fun blankQueryMatchesEveryAccount() {
        assertTrue(accounts.all { it.matchesQuery("") })
        assertTrue(accounts.all { it.matchesQuery("   ") })
    }

    @Test
    fun matchesByLabelCaseInsensitive() {
        assertEquals(1, accounts.count { it.matchesQuery("techstart") })
        assertEquals(1, accounts.count { it.matchesQuery("SAVINGS") })
    }

    @Test
    fun matchesByNumber() {
        val hits = accounts.filter { it.matchesQuery("2002") }
        assertEquals(1, hits.size)
        assertEquals("ac.savings.001", hits.single().id)
    }

    @Test
    fun matchesByIbanAndIdentifier() {
        assertTrue(accounts.single { it.id == "ac.checking.001" }.matchesQuery("gb00mifos1001"))
    }

    @Test
    fun matchesByAccountId() {
        assertEquals(1, accounts.count { it.matchesQuery("techstart.current") })
    }

    @Test
    fun noMatchReturnsEmpty() {
        assertTrue(accounts.none { it.matchesQuery("nonexistent-xyz") })
        assertFalse(accounts.first().matchesQuery("9999"))
    }
}
