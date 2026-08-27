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

import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_credit
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_current
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_global_money
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_other
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_account_type_savings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [accountFallbackLabel] — the non-composable identifier fallback shown when the bank supplies
 * no holder name. The `@Composable accountDisplayName` only resolves the localized type label and
 * delegates here, so this is where the card-vs-account behaviour is asserted.
 */
class AccountDisplayNameTest {

    @Test
    fun aGlobalMoneyWalletResolvesToItsOwnLabelDespiteReportingCacc() {
        assertEquals(
            Res.string.core_ui_account_type_global_money,
            accountTypeLabelRes(accountTypeCode = "CACC", description = "GLOBAL MONEY ACCOUNT"),
        )
    }

    @Test
    fun aPlainCurrentAccountIsUnaffectedByTheGlobalMoneyMarker() {
        assertEquals(
            Res.string.core_ui_account_type_current,
            accountTypeLabelRes(accountTypeCode = "CACC", description = "Description of the account"),
        )
    }

    @Test
    fun theRemainingProductCodesResolveToTheirLabels() {
        assertEquals(
            Res.string.core_ui_account_type_savings,
            accountTypeLabelRes(accountTypeCode = "SVGS", description = ""),
        )
        assertEquals(
            Res.string.core_ui_account_type_credit,
            accountTypeLabelRes(accountTypeCode = "CARD", description = ""),
        )
        assertEquals(
            Res.string.core_ui_account_type_other,
            accountTypeLabelRes(accountTypeCode = "XXXX", description = ""),
        )
    }

    @Test
    fun aCreditCardShowsTheBankMaskedIdentification() {
        val label = accountFallbackLabel(
            typeLabel = "Credit card",
            accountTypeCode = "CARD",
            scheme = AccountScheme.Pan,
            identification = "xxxx-xxxx-xxxx-3456",
        )

        assertEquals("Credit card xxxx-xxxx-xxxx-3456", label)
    }

    @Test
    fun aCardTypeCodeIsTreatedAsACardEvenWithoutThePanScheme() {
        listOf("card", "ccrd", "CARD", "CCRD").forEach { typeCode ->
            val label = accountFallbackLabel(
                typeLabel = "Credit card",
                accountTypeCode = typeCode,
                scheme = AccountScheme.SortCode,
                identification = "xxxx-xxxx-xxxx-3456",
            )
            assertEquals(
                "Credit card xxxx-xxxx-xxxx-3456",
                label,
                "type code $typeCode should be treated as a card",
            )
        }
    }

    @Test
    fun aCreditCardWithNoIdentificationFallsBackToJustTheTypeLabel() {
        val label = accountFallbackLabel(
            typeLabel = "Credit card",
            accountTypeCode = "CARD",
            scheme = AccountScheme.Pan,
            identification = "",
        )

        assertEquals("Credit card", label)
    }

    @Test
    fun aCurrentAccountKeepsTheTypeAndLastFourForm() {
        val label = accountFallbackLabel(
            typeLabel = "Current account",
            accountTypeCode = "CACC",
            scheme = AccountScheme.SortCode,
            identification = "40051512343349",
        )

        assertEquals("Current account ·· 3349", label)
    }

    @Test
    fun aNonCardUsesTheIdentificationLastFour() {
        val label = accountFallbackLabel(
            typeLabel = "Global money",
            accountTypeCode = "CACC",
            scheme = AccountScheme.SortCode,
            identification = "80119770009652",
        )

        assertEquals("Global money ·· 9652", label)
    }

    @Test
    fun aNonCardWithNoIdentificationFallsBackToJustTheTypeLabel() {
        val label = accountFallbackLabel(
            typeLabel = "Account",
            accountTypeCode = "",
            scheme = AccountScheme.Other,
            identification = "",
        )

        assertEquals("Account", label)
    }

    @Test
    fun isCreditCardRecognisesThePanSchemeAndCardTypeCodes() {
        assertTrue(isCreditCard(AccountScheme.Pan, ""))
        assertTrue(isCreditCard(AccountScheme.SortCode, "CARD"))
        assertTrue(isCreditCard(AccountScheme.SortCode, "ccrd"))
        assertFalse(isCreditCard(AccountScheme.SortCode, "CACC"))
        assertFalse(isCreditCard(AccountScheme.SortCode, "SVGS"))
        assertFalse(isCreditCard(AccountScheme.Other, ""))
    }
}
