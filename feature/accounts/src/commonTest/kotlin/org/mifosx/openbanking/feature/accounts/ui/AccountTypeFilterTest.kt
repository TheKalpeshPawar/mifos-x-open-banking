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

class AccountTypeFilterTest {

    private fun account(type: String) = Account(id = type, accountType = type)

    @Test
    fun allMatchesEveryAccount() {
        val accounts = listOf(account("CHECKING"), account("SAVINGS"), account("BUSINESS"), account(""))
        assertTrue(accounts.all(AccountTypeFilter.ALL::matches))
    }

    @Test
    fun checkingMatchesOnlyCheckingCaseInsensitive() {
        assertTrue(AccountTypeFilter.CHECKING.matches(account("checking")))
        assertTrue(AccountTypeFilter.CHECKING.matches(account("Primary Checking")))
        assertFalse(AccountTypeFilter.CHECKING.matches(account("SAVINGS")))
    }

    @Test
    fun savingsAndBusinessMatchTheirOwnType() {
        assertTrue(AccountTypeFilter.SAVINGS.matches(account("savings")))
        assertTrue(AccountTypeFilter.BUSINESS.matches(account("Business Current")))
        assertFalse(AccountTypeFilter.SAVINGS.matches(account("CHECKING")))
        assertFalse(AccountTypeFilter.BUSINESS.matches(account("savings")))
    }

    @Test
    fun filteringAListAppliesPredicate() {
        val accounts = listOf(account("CHECKING"), account("SAVINGS"), account("CHECKING"))
        assertEquals(2, accounts.count(AccountTypeFilter.CHECKING::matches))
        assertEquals(3, accounts.count(AccountTypeFilter.ALL::matches))
        assertEquals(0, accounts.count(AccountTypeFilter.BUSINESS::matches))
    }
}
