/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentlist

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.ConsentSummary
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.feature.consentlist.ui.ConsentCardUi
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListErrorKind
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListState
import org.mifosx.openbanking.feature.consentlist.ui.ConsentListUiState
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The consent-list states the suites render, carrying the HSBC-sandbox demo set the design was drawn
 * against.
 *
 * The screen shows the single current consent. Three variants exercise every branch: one comfortably
 * in date (89 days, no urgency chip), one inside the 14-day reconfirmation window (7 days, urgency
 * chip and banner), and one expired (needs renewing).
 *
 * Dates are anchored to [NOW] rather than the wall clock so the day arithmetic is deterministic.
 */
@OptIn(ExperimentalTime::class)
object ConsentListFixtures {

    /** The instant every fixture date is measured from. */
    val NOW: Instant = Instant.parse("2026-06-29T00:00:00Z")

    const val ACTIVE_ID: String = "aac-fb2c4e8a-7d31-4c9e-9f2a-1b3c5d7e9f01"
    const val NEAR_EXPIRY_ID: String = "aac-d4e5f6a7-8b9c-4d0e-1f2a-3b4c5d6e7f08"
    const val EXPIRED_ID: String = "aac-a1b2c3d4-e5f6-4789-0abc-def012345678"

    const val ACTIVE_DAYS: Int = 89
    const val NEAR_EXPIRY_DAYS: Int = 7
    const val ACTIVE_PERMISSION_COUNT: Int = 10

    // ── Domain models the stream hands the view model ────────────────────────────

    /** 89 days out, 10 permissions — the comfortable case, no urgency chip. */
    fun activeConsent(): ConsentSummary = ConsentSummary(
        consentId = ACTIVE_ID,
        status = ConsentStatus.Authorised,
        permissions = List(ACTIVE_PERMISSION_COUNT) { index -> "Permission$index" },
        creationDateTime = "2026-06-28T18:25:00Z",
        expirationDateTime = "2026-09-26T00:00:00Z",
        transactionFromDateTime = "2026-03-30T00:00:00Z",
        transactionToDateTime = "2026-06-28T23:59:59Z",
    )

    /** 7 days out — inside the 14-day window, so this one raises the banner. */
    fun nearExpiryConsent(): ConsentSummary = ConsentSummary(
        consentId = NEAR_EXPIRY_ID,
        status = ConsentStatus.Authorised,
        permissions = List(4) { index -> "Permission$index" },
        creationDateTime = "2026-04-07T09:00:00Z",
        expirationDateTime = "2026-07-06T00:00:00Z",
        transactionFromDateTime = "2026-01-07T00:00:00Z",
        transactionToDateTime = "2026-04-07T23:59:59Z",
    )

    /** Already lapsed — the current connection needs renewing. */
    fun expiredConsent(): ConsentSummary = ConsentSummary(
        consentId = EXPIRED_ID,
        status = ConsentStatus.Expired,
        permissions = List(3) { index -> "Permission$index" },
        creationDateTime = "2026-03-12T11:00:00Z",
        expirationDateTime = "2026-06-26T00:00:00Z",
        transactionFromDateTime = "2025-12-12T00:00:00Z",
        transactionToDateTime = "2026-03-12T23:59:59Z",
    )

    fun contentStreamState(): ScreenState<ConsentSummary> =
        ScreenState.Content(data = activeConsent(), freshness = DataFreshness.FRESH)

    fun nearExpiryStreamState(): ScreenState<ConsentSummary> =
        ScreenState.Content(data = nearExpiryConsent(), freshness = DataFreshness.FRESH)

    fun expiredStreamState(): ScreenState<ConsentSummary> =
        ScreenState.Content(data = expiredConsent(), freshness = DataFreshness.FRESH)

    fun errorStreamState(error: NetworkError): ScreenState<ConsentSummary> =
        ScreenState.Error(RemoteException(error))

    // ── Rendered UI states for the Compose suites ────────────────────────────────

    fun activeCard(): ConsentCardUi = ConsentCardUi(
        consentId = ACTIVE_ID,
        status = ConsentStatus.Authorised,
        permissionCount = ACTIVE_PERMISSION_COUNT,
        daysUntilExpiry = ACTIVE_DAYS,
        expiredOnDate = null,
        connectedDate = "28 Jun 2026",
        isNearExpiry = false,
    )

    fun nearExpiryCard(): ConsentCardUi = ConsentCardUi(
        consentId = NEAR_EXPIRY_ID,
        status = ConsentStatus.Authorised,
        permissionCount = 4,
        daysUntilExpiry = NEAR_EXPIRY_DAYS,
        expiredOnDate = null,
        connectedDate = "7 Apr 2026",
        isNearExpiry = true,
    )

    fun loadingState(): ConsentListState = ConsentListState(uiState = ConsentListUiState.Loading)

    /** The current consent, inside the reconfirmation window so the banner is raised. */
    fun contentState(): ConsentListState = ConsentListState(
        uiState = ConsentListUiState.Content(
            active = listOf(nearExpiryCard()),
            showReconfirmBanner = true,
        ),
    )

    /** The current consent, comfortably in date — no banner. */
    fun activeOnlyState(): ConsentListState = ConsentListState(
        uiState = ConsentListUiState.Content(
            active = listOf(activeCard()),
            showReconfirmBanner = false,
        ),
    )

    fun emptyState(): ConsentListState = ConsentListState(uiState = ConsentListUiState.Empty)

    fun errorState(
        kind: ConsentListErrorKind = ConsentListErrorKind.NetworkError,
    ): ConsentListState = ConsentListState(uiState = ConsentListUiState.Error(kind))

    fun authErrorState(): ConsentListState = ConsentListState(uiState = ConsentListUiState.ErrorAuth)
}
