/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.ConsentSummary
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailErrorKind
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailState
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailUi
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailUiState
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The consent-detail states the suites render, carrying the HSBC-sandbox demo consent the design was
 * drawn against: ten permissions, 89 days to expiry, dates anchored to [NOW] so the arithmetic is
 * deterministic.
 */
@OptIn(ExperimentalTime::class)
object ConsentDetailFixtures {

    val NOW: Instant = Instant.parse("2026-06-29T00:00:00Z")

    const val CONSENT_ID: String = "aac-fb2c4e8a-7d31-4c9e-9f2a-1b3c5d7e9f01"
    const val PERMISSION_COUNT: Int = 10
    const val FIRST_PERMISSION: String = "ReadAccountsDetail"

    fun permissions(): List<String> = listOf(
        FIRST_PERMISSION,
        "ReadBalances",
        "ReadTransactionsDetail",
        "ReadBeneficiariesDetail",
        "ReadStandingOrdersDetail",
        "ReadDirectDebits",
        "ReadScheduledPaymentsDetail",
        "ReadStatementsDetail",
        "ReadProducts",
        "ReadPartyPSU",
    )

    /** 89 days out — comfortably outside the 7-day warning window. */
    fun consent(): ConsentSummary = ConsentSummary(
        consentId = CONSENT_ID,
        status = ConsentStatus.Authorised,
        permissions = permissions(),
        creationDateTime = "2026-06-28T18:25:00Z",
        expirationDateTime = "2026-09-26T00:00:00Z",
        transactionFromDateTime = "2026-03-30T00:00:00Z",
        transactionToDateTime = "2026-06-28T23:59:59Z",
    )

    /** 3 days out — inside the warning window, so the banner renders. */
    fun nearExpiryConsent(): ConsentSummary = consent().copy(expirationDateTime = "2026-07-02T00:00:00Z")

    fun contentStreamState(): ScreenState<ConsentSummary> =
        ScreenState.Content(data = consent(), freshness = DataFreshness.FRESH)

    fun errorStreamState(error: NetworkError): ScreenState<ConsentSummary> =
        ScreenState.Error(RemoteException(error))

    // ── Rendered UI states for the Compose suites ────────────────────────────────

    fun consentUi(expiryWarningDays: Int? = null): ConsentDetailUi = ConsentDetailUi(
        consentId = CONSENT_ID,
        status = ConsentStatus.Authorised,
        permissions = permissions(),
        connectedDate = "28 Jun 2026",
        expiresDate = "26 Sep 2026",
        transactionFromDate = "30 Mar 2026",
        transactionToDate = "28 Jun 2026",
        expiryWarningDays = expiryWarningDays,
    )

    fun loadingState(): ConsentDetailState =
        ConsentDetailState(consentId = CONSENT_ID, uiState = ConsentDetailUiState.Loading)

    fun contentState(expiryWarningDays: Int? = null): ConsentDetailState = ConsentDetailState(
        consentId = CONSENT_ID,
        uiState = ConsentDetailUiState.Content(consentUi(expiryWarningDays)),
    )

    fun revokeConfirmState(): ConsentDetailState = ConsentDetailState(
        consentId = CONSENT_ID,
        uiState = ConsentDetailUiState.RevokeConfirm(consentUi()),
    )

    fun revokingState(): ConsentDetailState = ConsentDetailState(
        consentId = CONSENT_ID,
        uiState = ConsentDetailUiState.Revoking(consentUi()),
    )

    fun emptyState(): ConsentDetailState =
        ConsentDetailState(consentId = CONSENT_ID, uiState = ConsentDetailUiState.Empty)

    fun errorState(
        kind: ConsentDetailErrorKind = ConsentDetailErrorKind.ConsentNotFound,
    ): ConsentDetailState = ConsentDetailState(
        consentId = CONSENT_ID,
        uiState = ConsentDetailUiState.Error(kind),
    )
}
