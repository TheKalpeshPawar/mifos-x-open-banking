/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking

import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductCapability
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The resolver and the capability matrix.
 *
 * Every product gets its own case rather than a table-driven loop, for the reason the mapper suite
 * gives: a table that silently drops a row produces a plausible-looking wrong answer. The expected
 * values are the ones observed against `customer01`'s five sandbox accounts, so a regression here
 * means the app and the bank have diverged.
 */
class HsbcProductCapabilityTest {

    // --- resolver -----------------------------------------------------------------------------

    /**
     * The load-bearing case, pinning sandbox account `1123456843`.
     *
     * OBIE has no account-type code for a multi-currency wallet, so Global Money reports `CACC` —
     * identical to a real current account. If the type code is consulted before the description
     * this resolves to PersonalCurrentAccount and the chips wrongly come back.
     */
    @Test
    fun resolvesGlobalMoneyFromTheDescriptionEvenWhenTheTypeCodeSaysCacc() {
        assertEquals(
            HsbcProductType.GlobalMoney,
            HsbcProductType.resolve(accountTypeCode = "CACC", description = "GLOBAL MONEY ACCOUNT"),
        )
    }

    @Test
    fun resolvesGlobalMoneyRegardlessOfCase() {
        assertEquals(
            HsbcProductType.GlobalMoney,
            HsbcProductType.resolve("CACC", "global money account"),
        )
    }

    @Test
    fun resolvesPersonalCurrentAccountFromCaccWhenTheDescriptionIsOrdinary() {
        assertEquals(
            HsbcProductType.PersonalCurrentAccount,
            HsbcProductType.resolve("CACC", "Description of the account"),
        )
    }

    @Test
    fun resolvesSavingsFromTheSvgsCode() {
        assertEquals(HsbcProductType.Savings, HsbcProductType.resolve("SVGS", "BMM ACCOUNT"))
    }

    @Test
    fun resolvesCreditCardFromTheCardCode() {
        assertEquals(HsbcProductType.CreditCard, HsbcProductType.resolve("CARD", ""))
    }

    @Test
    fun resolvesUnknownForAnUnrecognisedProduct() {
        assertEquals(HsbcProductType.Unknown, HsbcProductType.resolve("", ""))
        assertEquals(HsbcProductType.Unknown, HsbcProductType.resolve("XXXX", "New"))
    }

    // --- matrix -------------------------------------------------------------------------------

    @Test
    fun personalCurrentAccountsSupportBothStandingOrdersAndDirectDebits() {
        val pca = HsbcProductType.PersonalCurrentAccount
        assertTrue(HsbcProductCapability.supports(AccountEndpoint.StandingOrders, pca))
        assertTrue(HsbcProductCapability.supports(AccountEndpoint.DirectDebits, pca))
    }

    @Test
    fun savingsSupportsNeitherStandingOrdersNorDirectDebits() {
        val savings = HsbcProductType.Savings
        assertFalse(HsbcProductCapability.supports(AccountEndpoint.StandingOrders, savings))
        assertFalse(HsbcProductCapability.supports(AccountEndpoint.DirectDebits, savings))
    }

    @Test
    fun creditCardSupportsNeitherStandingOrdersNorDirectDebits() {
        val card = HsbcProductType.CreditCard
        assertFalse(HsbcProductCapability.supports(AccountEndpoint.StandingOrders, card))
        assertFalse(HsbcProductCapability.supports(AccountEndpoint.DirectDebits, card))
    }

    @Test
    fun globalMoneySupportsNeitherStandingOrdersNorDirectDebits() {
        val wallet = HsbcProductType.GlobalMoney
        assertFalse(HsbcProductCapability.supports(AccountEndpoint.StandingOrders, wallet))
        assertFalse(HsbcProductCapability.supports(AccountEndpoint.DirectDebits, wallet))
    }

    /**
     * The asymmetry between the two endpoints is real and documented, not an oversight: standing
     * orders may run from a foreign-currency account, but direct debits are a sterling-scheme
     * instrument and may not.
     */
    @Test
    fun foreignCurrencySupportsStandingOrdersButNotDirectDebits() {
        val fx = HsbcProductType.ForeignCurrency
        assertTrue(HsbcProductCapability.supports(AccountEndpoint.StandingOrders, fx))
        assertFalse(HsbcProductCapability.supports(AccountEndpoint.DirectDebits, fx))
    }

    /**
     * Statements is the only credit-card-only endpoint: HSBC exposes a statement document over AIS
     * for credit cards and nothing else, so every other product hides the Statements chip.
     */
    @Test
    fun creditCardSupportsStatements() {
        assertTrue(HsbcProductCapability.supports(AccountEndpoint.Statements, HsbcProductType.CreditCard))
    }

    @Test
    fun personalCurrentAccountDoesNotSupportStatements() {
        assertFalse(
            HsbcProductCapability.supports(AccountEndpoint.Statements, HsbcProductType.PersonalCurrentAccount),
        )
    }

    @Test
    fun balancesAndTransactionsAreSupportedByEveryProduct() {
        HsbcProductType.entries.forEach { product ->
            assertTrue(
                HsbcProductCapability.supports(AccountEndpoint.Balances, product),
                "balances must never be hidden, but were for $product",
            )
            assertTrue(
                HsbcProductCapability.supports(AccountEndpoint.Transactions, product),
                "transactions must never be hidden, but were for $product",
            )
        }
    }

    /**
     * An unclassifiable product keeps everything and is corrected at runtime by `U000`. The table
     * only ever subtracts, so a wrongly restrictive guess would be undiscoverable in production
     * whereas a wrongly permissive one self-corrects on first use.
     */
    @Test
    fun anUnknownProductIsTreatedAsSupportingEverything() {
        AccountEndpoint.entries.forEach { endpoint ->
            assertTrue(
                HsbcProductCapability.supports(endpoint, HsbcProductType.Unknown),
                "$endpoint must stay available for an unknown product",
            )
        }
    }
}
