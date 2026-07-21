/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.network.model.ais.accountDetails.Account
import org.mifosx.openbanking.core.network.model.ais.accountDetails.AccountDetailsResponse
import org.mifosx.openbanking.core.network.model.ais.accountDetails.Servicer
import org.mifosx.openbanking.core.network.model.ais.balances.Amount
import org.mifosx.openbanking.core.network.model.ais.balances.Balance
import org.mifosx.openbanking.core.network.model.ais.balances.BalancesResponse
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mifosx.openbanking.core.network.model.ais.accountDetails.Data as AccountDetailsData
import org.mifosx.openbanking.core.network.model.ais.balances.Data as BalancesData

/**
 * Covers the OBIE detail and balance payload mappers.
 *
 * Both are defensive by design: fields the bank may omit fall back down a documented chain, and a
 * row that cannot render is dropped rather than surfaced half-built. Each fallback rung is asserted
 * separately, because a chain that silently skips a rung produces a plausible-looking wrong value.
 */
class AccountDetailMapperTest {

    private fun response(vararg accounts: Account) =
        AccountDetailsResponse(data = AccountDetailsData(account = accounts.toList()))

    private fun balancesResponse(vararg rows: Balance) =
        BalancesResponse(data = BalancesData(balance = rows.toList()))

    @Test
    fun toAccountDetailMapsAFullyPopulatedAccount() {
        val detail = response(
            Account(
                accountId = "acc-1",
                currency = "GBP",
                accountSubType = "CurrentAccount",
                nickname = "Everyday Current",
                statusUpdateDateTime = "2026-06-28T18:30:00Z",
                servicer = Servicer(schemeName = "UK.OBIE.BICFI", identification = "MIDLGB2105V"),
                account = listOf(
                    Account(schemeName = "UK.OBIE.SortCodeAccountNumber", identification = "40051512345678"),
                ),
            ),
        ).toAccountDetail()

        assertEquals("acc-1", detail?.accountId)
        assertEquals("Everyday Current", detail?.nickname)
        assertEquals("CurrentAccount", detail?.accountSubType)
        assertEquals("GBP", detail?.currency)
        assertEquals("400515", detail?.sortCode)
        assertEquals("12345678", detail?.accountNumber)
        assertEquals("MIDLGB2105V", detail?.servicerIdentification)
        assertEquals("2026-06-28T18:30:00Z", detail?.statusUpdateDateTime)
    }

    @Test
    fun toAccountDetailFallsBackFromNicknameToNameToDescriptionToId() {
        fun nicknameOf(account: Account): String? = response(account).toAccountDetail()?.nickname

        assertEquals("Nick", nicknameOf(Account(accountId = "acc-1", nickname = "Nick", name = "Name")))
        assertEquals("Name", nicknameOf(Account(accountId = "acc-1", name = "Name", description = "Desc")))
        assertEquals("Desc", nicknameOf(Account(accountId = "acc-1", description = "Desc")))
        assertEquals("acc-1", nicknameOf(Account(accountId = "acc-1")))
    }

    @Test
    fun toAccountDetailFallsBackThroughTheSubTypeChain() {
        val withSubType = Account(accountId = "a", accountSubType = "CurrentAccount", accountTypeCode = "TC")
        assertEquals("CurrentAccount", response(withSubType).toAccountDetail()?.accountSubType)

        val withTypeCode = Account(accountId = "a", accountTypeCode = "TC", accountCategory = "Personal")
        assertEquals("TC", response(withTypeCode).toAccountDetail()?.accountSubType)

        val withCategory = Account(accountId = "a", accountCategory = "Personal", description = "Desc")
        assertEquals("Personal", response(withCategory).toAccountDetail()?.accountSubType)

        assertEquals("Desc", response(Account(accountId = "a", description = "Desc")).toAccountDetail()?.accountSubType)
        assertEquals("", response(Account(accountId = "a")).toAccountDetail()?.accountSubType)
    }

    /**
     * Carried verbatim, deliberately bypassing the fallback chain above.
     *
     * Product classification has to know which field a value came from — `CACC` in `AccountTypeCode`
     * means something different from `CACC` arriving via `resolveSubType`'s fallback — and the chain
     * collapses exactly that distinction.
     */
    @Test
    fun toAccountDetailCarriesTheAccountTypeCodeAndDescriptionVerbatim() {
        val account = Account(
            accountId = "a",
            accountSubType = "CurrentAccount",
            accountTypeCode = "CACC",
            description = "GLOBAL MONEY ACCOUNT",
        )
        val detail = response(account).toAccountDetail()

        assertEquals("CACC", detail?.accountTypeCode)
        assertEquals("GLOBAL MONEY ACCOUNT", detail?.description)
        // The lossy chain still wins for accountSubType — the two live side by side.
        assertEquals("CurrentAccount", detail?.accountSubType)
    }

    @Test
    fun toAccountDetailDefaultsTheAccountTypeCodeAndDescriptionToEmptyStrings() {
        val detail = response(Account(accountId = "a")).toAccountDetail()

        assertEquals("", detail?.accountTypeCode)
        assertEquals("", detail?.description)
    }

    @Test
    fun toAccountDetailPrefersTheSortCodeSchemeOverTheFirstNestedEntry() {
        val detail = response(
            Account(
                accountId = "acc-1",
                account = listOf(
                    Account(schemeName = "UK.OBIE.IBAN", identification = "GB99MIDL99999999999999"),
                    Account(schemeName = "UK.OBIE.SortCodeAccountNumber", identification = "40051512345678"),
                ),
            ),
        ).toAccountDetail()

        assertEquals("400515", detail?.sortCode)
        assertEquals("12345678", detail?.accountNumber)
    }

    @Test
    fun toAccountDetailMatchesTheSortCodeSchemeCaseInsensitively() {
        val detail = response(
            Account(
                accountId = "acc-1",
                account = listOf(
                    Account(schemeName = "uk.obie.sortcodeaccountnumber", identification = "40051512345678"),
                ),
            ),
        ).toAccountDetail()

        assertEquals("400515", detail?.sortCode)
    }

    @Test
    fun toAccountDetailUsesTheFirstNestedEntryWhenNoSortCodeSchemeIsPresent() {
        val detail = response(
            Account(
                accountId = "acc-1",
                account = listOf(
                    Account(schemeName = "UK.OBIE.IBAN", identification = "11111122222222"),
                    Account(schemeName = "UK.OBIE.PAN", identification = "99999988888888"),
                ),
            ),
        ).toAccountDetail()

        assertEquals("111111", detail?.sortCode)
        assertEquals("22222222", detail?.accountNumber)
    }

    @Test
    fun toAccountDetailFallsBackToTheTopLevelIdentification() {
        val detail = response(Account(accountId = "acc-1", identification = "40051512345678")).toAccountDetail()

        assertEquals("400515", detail?.sortCode)
        assertEquals("12345678", detail?.accountNumber)
    }

    @Test
    fun toAccountDetailLeavesBothIdentificationHalvesBlankWhenNoIdentificationIsPresent() {
        val detail = response(Account(accountId = "acc-1")).toAccountDetail()

        assertEquals("", detail?.sortCode)
        assertEquals("", detail?.accountNumber)
    }

    @Test
    fun toAccountDetailTruncatesAnOverlongIdentification() {
        val detail = response(Account(accountId = "acc-1", identification = "400515123456789999")).toAccountDetail()

        assertEquals("400515", detail?.sortCode)
        assertEquals("12345678", detail?.accountNumber)
    }

    @Test
    fun toAccountDetailDefaultsServicerAndStatusTimestampToEmptyStrings() {
        val detail = response(Account(accountId = "acc-1")).toAccountDetail()

        assertEquals("", detail?.servicerIdentification)
        assertEquals("", detail?.statusUpdateDateTime)
        assertEquals("", detail?.currency)
    }

    @Test
    fun toAccountDetailDefaultsServicerIdentificationWhenTheServicerCarriesNoIdentification() {
        val detail = response(Account(accountId = "acc-1", servicer = Servicer(schemeName = "UK.OBIE.BICFI")))
            .toAccountDetail()

        assertEquals("", detail?.servicerIdentification)
    }

    @Test
    fun toAccountDetailSkipsEntriesWithoutAnAccountIdAndTakesTheFirstUsableOne() {
        val detail = response(
            Account(nickname = "no id here"),
            Account(accountId = "acc-2", nickname = "usable"),
        ).toAccountDetail()

        assertEquals("acc-2", detail?.accountId)
        assertEquals("usable", detail?.nickname)
    }

    @Test
    fun toAccountDetailIsNullWhenNoEntryCarriesAnAccountId() {
        assertNull(response(Account(nickname = "no id")).toAccountDetail())
    }

    @Test
    fun toAccountDetailIsNullForAnEmptyOrAbsentPayload() {
        assertNull(response().toAccountDetail())
        assertNull(AccountDetailsResponse(data = AccountDetailsData(account = null)).toAccountDetail())
        assertNull(AccountDetailsResponse().toAccountDetail())
    }

    @Test
    fun toAccountBalanceLinesMapsEveryRowPreservingOrder() {
        val lines = balancesResponse(
            Balance(
                type = "InterimAvailable",
                dateTime = "2026-06-28T18:30:00Z",
                amount = Amount(amount = "2847.63", currency = "GBP"),
            ),
            Balance(
                type = "InterimBooked",
                dateTime = "2026-06-28T18:31:00Z",
                amount = Amount(amount = "2905.10", currency = "GBP"),
            ),
        ).toAccountBalanceLines()

        assertContentEquals(listOf("InterimAvailable", "InterimBooked"), lines.map { it.type })
        assertEquals("2847.63", lines[0].amount)
        assertEquals("GBP", lines[0].currency)
        assertEquals("2026-06-28T18:30:00Z", lines[0].dateTime)
        assertEquals("2905.10", lines[1].amount)
    }

    @Test
    fun toAccountBalanceLinesDropsRowsWithoutAType() {
        val lines = balancesResponse(
            Balance(type = null, amount = Amount(amount = "1.00", currency = "GBP")),
            Balance(type = "InterimBooked", amount = Amount(amount = "2.00", currency = "GBP")),
        ).toAccountBalanceLines()

        assertEquals(1, lines.size)
        assertEquals("InterimBooked", lines.single().type)
    }

    @Test
    fun toAccountBalanceLinesDropsRowsWithoutAnAmountValue() {
        val lines = balancesResponse(
            Balance(type = "OpeningBooked", amount = Amount(amount = null, currency = "GBP")),
            Balance(type = "ClosingBooked", amount = null),
            Balance(type = "InterimAvailable", amount = Amount(amount = "3.00", currency = "GBP")),
        ).toAccountBalanceLines()

        assertEquals(1, lines.size)
        assertEquals("InterimAvailable", lines.single().type)
    }

    @Test
    fun toAccountBalanceLinesDefaultsAMissingCurrencyAndDateTimeToEmptyStrings() {
        val lines = balancesResponse(Balance(type = "InterimAvailable", amount = Amount(amount = "5.00")))
            .toAccountBalanceLines()

        assertEquals("", lines.single().currency)
        assertEquals("", lines.single().dateTime)
    }

    @Test
    fun toAccountBalanceLinesIsEmptyForAnEmptyOrAbsentPayload() {
        assertTrue(balancesResponse().toAccountBalanceLines().isEmpty())
        assertTrue(BalancesResponse(data = BalancesData(balance = null)).toAccountBalanceLines().isEmpty())
        assertTrue(BalancesResponse().toAccountBalanceLines().isEmpty())
    }
}
