/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesErrorKind
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesState
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesUiState
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiaryRowUi
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError

/**
 * The beneficiaries states the suites render, carrying the HSBC-sandbox demo set the design was
 * drawn against (accountId `40051512345678`).
 *
 * Five payees, chosen so the interesting cases are all present in one list: four sort-code payees
 * and one IBAN payee (which is the only scheme that gets display grouping), a two-word name and a
 * name whose second word is numeric (`Priya Rajan N26 GmbH`, whose initials must be `PR`, not `PN`),
 * and references that make the search cases unambiguous.
 *
 * Shared by the view-model, Compose, Robolectric and screenshot suites so all assert against one
 * screen rather than several that happen to look alike.
 */
object BeneficiariesFixtures {

    const val ACCOUNT_ID: String = "40051512345678"

    const val FIRST_ID: String = "BEN-001"
    const val ENERGY_ID: String = "BEN-003"
    const val ISA_ID: String = "BEN-004"
    const val IBAN_ID: String = "BEN-005"

    const val EXPECTED_COUNT: Int = 5

    /** A query matching exactly one payee by name — `EDF Energy`. */
    const val NAME_QUERY: String = "ener"

    /** A query matching exactly one payee by reference — `ISA-TOPUP`. Upper case on purpose. */
    const val REFERENCE_QUERY: String = "ISA"

    /** A query matching nothing, for the in-content no-results block. */
    const val NO_MATCH_QUERY: String = "zzzmatch"

    // ── Domain models the stream hands the view model ────────────────────────────

    fun beneficiaryList(): List<BeneficiaryItem> = listOf(
        item(FIRST_ID, "Jameson Lettings", BeneficiaryScheme.SortCode, "40-12-09 65872310", "RENT-FLAT12"),
        item("BEN-002", "John Sharma", BeneficiaryScheme.SortCode, "23-05-80 11223344", "FAMILY"),
        item(ENERGY_ID, "EDF Energy", BeneficiaryScheme.SortCode, "60-00-01 99887766", "ELEC-8841"),
        item(ISA_ID, "Hargreaves Lansdown", BeneficiaryScheme.SortCode, "11-22-33 44556677", "ISA-TOPUP"),
        item(IBAN_ID, "Priya Rajan N26 GmbH", BeneficiaryScheme.Iban, "DE89370400440532013000", "TRAVEL-EUR"),
    )

    fun item(
        id: String,
        name: String,
        scheme: BeneficiaryScheme,
        identification: String,
        reference: String,
    ): BeneficiaryItem = BeneficiaryItem(
        beneficiaryId = id,
        accountId = ACCOUNT_ID,
        creditorName = name,
        scheme = scheme,
        identification = identification,
        reference = reference,
    )

    fun contentStreamState(): ScreenState<List<BeneficiaryItem>> =
        ScreenState.Content(data = beneficiaryList(), freshness = DataFreshness.FRESH)

    fun emptyStreamState(): ScreenState<List<BeneficiaryItem>> =
        ScreenState.Content(data = emptyList(), freshness = DataFreshness.FRESH)

    fun errorStreamState(error: NetworkError): ScreenState<List<BeneficiaryItem>> =
        ScreenState.Error(RemoteException(error))

    // ── Rendered UI states for the Compose suites ────────────────────────────────

    /** The five payees as the view model formats them — note the IBAN arrives grouped. */
    fun rows(): List<BeneficiaryRowUi> = listOf(
        row(FIRST_ID, "Jameson Lettings", "JL", BeneficiaryScheme.SortCode, "40-12-09 65872310", "RENT-FLAT12"),
        row("BEN-002", "John Sharma", "JS", BeneficiaryScheme.SortCode, "23-05-80 11223344", "FAMILY"),
        row(ENERGY_ID, "EDF Energy", "EE", BeneficiaryScheme.SortCode, "60-00-01 99887766", "ELEC-8841"),
        row(ISA_ID, "Hargreaves Lansdown", "HL", BeneficiaryScheme.SortCode, "11-22-33 44556677", "ISA-TOPUP"),
        row(IBAN_ID, "Priya Rajan N26 GmbH", "PR", BeneficiaryScheme.Iban, "DE89 3704 0044 0532 0130 00", "TRAVEL-EUR"),
    )

    fun row(
        id: String,
        name: String,
        initials: String,
        scheme: BeneficiaryScheme,
        identification: String,
        reference: String,
    ): BeneficiaryRowUi = BeneficiaryRowUi(
        beneficiaryId = id,
        name = name,
        initials = initials,
        scheme = scheme,
        identification = identification,
        reference = reference,
    )

    fun loadingState(): BeneficiariesState =
        BeneficiariesState(accountId = ACCOUNT_ID, uiState = BeneficiariesUiState.Loading)

    /** Content with no query — every row renders. */
    fun contentState(): BeneficiariesState = BeneficiariesState(
        accountId = ACCOUNT_ID,
        uiState = BeneficiariesUiState.Content(all = rows(), filtered = rows(), query = ""),
    )

    /** Content with a query that matched one payee. */
    fun searchedState(query: String = NAME_QUERY): BeneficiariesState = BeneficiariesState(
        accountId = ACCOUNT_ID,
        uiState = BeneficiariesUiState.Content(
            all = rows(),
            filtered = rows().filter { it.name.contains(query, ignoreCase = true) },
            query = query,
        ),
    )

    /** Content with a live query that matched nothing — the in-content no-results block. */
    fun searchWithoutMatchesState(): BeneficiariesState = BeneficiariesState(
        accountId = ACCOUNT_ID,
        uiState = BeneficiariesUiState.Content(all = rows(), filtered = emptyList(), query = NO_MATCH_QUERY),
    )

    fun emptyState(): BeneficiariesState =
        BeneficiariesState(accountId = ACCOUNT_ID, uiState = BeneficiariesUiState.Empty)

    fun errorState(
        kind: BeneficiariesErrorKind = BeneficiariesErrorKind.TokenExpired,
    ): BeneficiariesState = BeneficiariesState(
        accountId = ACCOUNT_ID,
        uiState = BeneficiariesUiState.Error(kind),
    )
}
