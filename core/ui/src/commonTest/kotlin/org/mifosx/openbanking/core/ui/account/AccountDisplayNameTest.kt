/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [accountFallbackLabel] — the non-composable identifier fallback shown when the bank supplies
 * no nickname. The `@Composable accountDisplayName` only resolves the localized type label and delegates
 * here, so this is where the credit-card-vs-account-number behaviour is asserted.
 */
class AccountDisplayNameTest {

    @Test
    fun aCreditCardShowsTheRealMaskedLastFourNotTheMidPanSlice() {
        // The mapper flattens a PAN into a mid-PAN accountNumber ("34123412"); the label must ignore it
        // and mask the real last four of the raw card number instead.
        val label = accountFallbackLabel(
            typeLabel = "Credit card",
            accountSubType = "CreditCard",
            accountNumber = "34123412",
            rawIdentification = "4111111111117654",
        )

        assertEquals("Credit card •••• 7654", label)
    }

    @Test
    fun everyCreditCardSubtypeTokenIsMasked() {
        listOf("creditcard", "credit", "card", "ccrd", "CreditCard", "CARD").forEach { subType ->
            val label = accountFallbackLabel(
                typeLabel = "Credit card",
                accountSubType = subType,
                accountNumber = "34123412",
                rawIdentification = "4111111111117654",
            )
            assertEquals("Credit card •••• 7654", label, "subtype $subType should mask the last four")
        }
    }

    @Test
    fun aCreditCardWithNoRawIdentificationFallsBackToJustTheTypeLabel() {
        val label = accountFallbackLabel(
            typeLabel = "Credit card",
            accountSubType = "CreditCard",
            accountNumber = "",
            rawIdentification = "",
        )

        assertEquals("Credit card", label)
    }

    @Test
    fun aCurrentAccountKeepsTheTypeAndLastFourForm() {
        val label = accountFallbackLabel(
            typeLabel = "Current account",
            accountSubType = "CurrentAccount",
            accountNumber = "12343349",
            rawIdentification = "40051512343349",
        )

        assertEquals("Current account ·· 3349", label)
    }

    @Test
    fun aNonCardWithoutAnAccountNumberUsesTheRawIdentificationLastFour() {
        val label = accountFallbackLabel(
            typeLabel = "Global money",
            accountSubType = "GlobalMoney",
            accountNumber = "",
            rawIdentification = "GB29HBUK40051512345678",
        )

        assertEquals("Global money ·· 5678", label)
    }

    @Test
    fun aNonCardWithNoDigitsAtAllFallsBackToJustTheTypeLabel() {
        val label = accountFallbackLabel(
            typeLabel = "Account",
            accountSubType = "Other",
            accountNumber = "",
            rawIdentification = "",
        )

        assertEquals("Account", label)
    }

    @Test
    fun isCreditCardSubTypeRecognisesTheObieTokensCaseInsensitivelyAndRejectsOthers() {
        assertTrue(isCreditCardSubType("CreditCard"))
        assertTrue(isCreditCardSubType("credit"))
        assertTrue(isCreditCardSubType("CARD"))
        assertTrue(isCreditCardSubType("ccrd"))
        assertFalse(isCreditCardSubType("CurrentAccount"))
        assertFalse(isCreditCardSubType("Savings"))
        assertFalse(isCreditCardSubType(""))
    }
}
