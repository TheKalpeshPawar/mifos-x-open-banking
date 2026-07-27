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

import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.network.model.ais.beneficiaries.BeneficiariesResponse
import org.mifosx.openbanking.core.network.model.ais.beneficiaries.Beneficiary
import org.mifosx.openbanking.core.network.model.ais.beneficiaries.CreditorAccount
import org.mifosx.openbanking.core.network.model.ais.beneficiaries.Data
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins [BeneficiariesResponse.toBeneficiaryItems]: the bank's order survives, every scheme string
 * resolves, and a payee missing an optional field still renders rather than vanishing.
 */
class BeneficiaryMapperTest {

    private fun response(vararg beneficiaries: Beneficiary) =
        BeneficiariesResponse(data = Data(beneficiary = beneficiaries.toList()))

    private fun beneficiary(
        id: String = "BEN-001",
        accountId: String? = ACCOUNT_ID,
        name: String? = "Jameson Lettings",
        scheme: String? = "UK.OBIE.SortCodeAccountNumber",
        identification: String? = "40-12-09 65872310",
        reference: String? = "RENT-FLAT12",
    ) = Beneficiary(
        accountId = accountId,
        beneficiaryId = id,
        reference = reference,
        creditorAccount = CreditorAccount(
            schemeName = scheme,
            identification = identification,
            name = name,
        ),
    )

    @Test
    fun everyFieldIsCarriedAcross() {
        val item = response(beneficiary()).toBeneficiaryItems(ACCOUNT_ID).single()

        assertEquals("BEN-001", item.beneficiaryId)
        assertEquals(ACCOUNT_ID, item.accountId)
        assertEquals("Jameson Lettings", item.creditorName)
        assertEquals(BeneficiaryScheme.SortCode, item.scheme)
        assertEquals("40-12-09 65872310", item.identification)
        assertEquals("RENT-FLAT12", item.reference)
    }

    @Test
    fun theBanksOrderIsPreserved() {
        val items = response(
            beneficiary(id = "BEN-003", name = "EDF Energy"),
            beneficiary(id = "BEN-001", name = "Jameson Lettings"),
            beneficiary(id = "BEN-002", name = "John Sharma"),
        ).toBeneficiaryItems(ACCOUNT_ID)

        assertEquals(listOf("BEN-003", "BEN-001", "BEN-002"), items.map { it.beneficiaryId })
    }

    @Test
    fun eachNamespacedSchemeResolves() {
        val schemes = listOf(
            "UK.OBIE.SortCodeAccountNumber" to BeneficiaryScheme.SortCode,
            "UK.OBIE.IBAN" to BeneficiaryScheme.Iban,
            "UK.OBIE.Paym" to BeneficiaryScheme.Paym,
            "UK.OBIE.PAN" to BeneficiaryScheme.Card,
        )

        schemes.forEach { (raw, expected) ->
            val item = response(beneficiary(scheme = raw)).toBeneficiaryItems(ACCOUNT_ID).single()
            assertEquals(expected, item.scheme, "scheme $raw")
        }
    }

    @Test
    fun aBareSchemeResolvesCaseInsensitively() {
        val item = response(beneficiary(scheme = "iban")).toBeneficiaryItems(ACCOUNT_ID).single()

        assertEquals(BeneficiaryScheme.Iban, item.scheme)
    }

    @Test
    fun anUnknownSchemeFallsBackToAccount() {
        val item = response(beneficiary(scheme = "UK.OBIE.SomethingElse")).toBeneficiaryItems(ACCOUNT_ID).single()

        assertEquals(BeneficiaryScheme.Account, item.scheme)
    }

    @Test
    fun aMissingSchemeFallsBackToAccount() {
        val item = response(beneficiary(scheme = null)).toBeneficiaryItems(ACCOUNT_ID).single()

        assertEquals(BeneficiaryScheme.Account, item.scheme)
    }

    @Test
    fun absentOptionalFieldsDegradeToEmptyStringsRatherThanDroppingThePayee() {
        val item = response(
            Beneficiary(accountId = null, beneficiaryId = null, reference = null, creditorAccount = null),
        ).toBeneficiaryItems(ACCOUNT_ID).single()

        assertEquals("", item.beneficiaryId)
        assertEquals("", item.creditorName)
        assertEquals("", item.identification)
        assertEquals("", item.reference)
        assertEquals(BeneficiaryScheme.Account, item.scheme)
    }

    @Test
    fun theRequestedAccountIdIsUsedWhenThePayloadOmitsItsOwn() {
        val item = response(beneficiary(accountId = null)).toBeneficiaryItems(ACCOUNT_ID).single()

        assertEquals(ACCOUNT_ID, item.accountId)
    }

    @Test
    fun aPayloadAccountIdWinsOverTheRequestedOne() {
        val item = response(beneficiary(accountId = "40051599999999")).toBeneficiaryItems(ACCOUNT_ID).single()

        assertEquals("40051599999999", item.accountId)
    }

    @Test
    fun anAbsentDataBlockMapsToAnEmptyList() {
        assertTrue(BeneficiariesResponse().toBeneficiaryItems(ACCOUNT_ID).isEmpty())
    }

    @Test
    fun anEmptyBeneficiaryArrayMapsToAnEmptyList() {
        assertTrue(response().toBeneficiaryItems(ACCOUNT_ID).isEmpty())
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
    }
}
