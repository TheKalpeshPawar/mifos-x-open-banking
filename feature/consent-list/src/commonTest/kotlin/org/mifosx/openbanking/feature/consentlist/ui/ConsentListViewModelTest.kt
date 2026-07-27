/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentlist.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.feature.consentlist.ConsentListFixtures
import org.mifosx.openbanking.feature.consentlist.FakeConsentDetailRepository
import org.mifosx.openbanking.feature.consentlist.FakeConsentSession
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Covers [ConsentListViewModel]'s current-consent sourcing, the single-card mapping, expiry
 * arithmetic, and the split between the generic error state and the dedicated expired-session one.
 *
 * Test names are camelCase — this source set also compiles for Kotlin/Native, whose frontend rejects
 * the punctuation a prose-style backticked name would carry.
 */
@OptIn(ExperimentalTime::class)
class ConsentListViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeConsentDetailRepository = FakeConsentDetailRepository(),
        currentConsentId: String? = ConsentListFixtures.ACTIVE_ID,
    ): ConsentListViewModel = ConsentListViewModel(
        repository = repository,
        session = FakeConsentSession(currentConsentId),
    )

    private fun contentRepository() = FakeConsentDetailRepository(ConsentListFixtures.contentStreamState())

    private fun errorRepository(error: NetworkError) =
        FakeConsentDetailRepository(ConsentListFixtures.errorStreamState(error))

    private fun content(vm: ConsentListViewModel): ConsentListUiState.Content =
        assertIs<ConsentListUiState.Content>(vm.stateFlow.value.uiState)

    // region — current-consent sourcing

    @Test
    fun theConsentIdComesFromTheSession() {
        val repository = contentRepository()

        viewModel(repository = repository, currentConsentId = ConsentListFixtures.NEAR_EXPIRY_ID)

        assertEquals(ConsentListFixtures.NEAR_EXPIRY_ID, repository.observedConsentId)
    }

    @Test
    fun noCurrentConsentShortCircuitsToEmptyWithoutAskingTheBank() {
        val repository = contentRepository()

        val vm = viewModel(repository = repository, currentConsentId = null)

        assertIs<ConsentListUiState.Empty>(vm.stateFlow.value.uiState)
        assertNull(repository.observedConsentId)
    }

    @Test
    fun theInitialStateIsLoadingWhenAConsentExists() {
        val vm = viewModel()

        assertIs<ConsentListUiState.Loading>(vm.stateFlow.value.uiState)
    }

    // endregion

    // region — the single card

    @Test
    fun theCurrentConsentRendersAsASingleCard() {
        val vm = viewModel(repository = contentRepository())

        assertEquals(listOf(ConsentListFixtures.ACTIVE_ID), content(vm).active.map { it.consentId })
    }

    @Test
    fun theCardReportsItsPermissionCount() {
        val vm = viewModel(repository = contentRepository())

        assertEquals(ConsentListFixtures.ACTIVE_PERMISSION_COUNT, content(vm).active.first().permissionCount)
    }

    @Test
    fun theCardCarriesNoExpiredOnDate() {
        val vm = viewModel(repository = contentRepository())

        assertNull(content(vm).active.first().expiredOnDate)
    }

    @Test
    fun theConnectedDateIsFormattedForDisplay() {
        val vm = viewModel(repository = contentRepository())

        assertEquals("28 Jun 2026", content(vm).active.first().connectedDate)
    }

    @Test
    fun theCardCarriesTheBankReportedStatus() {
        val revoked = ConsentListFixtures.activeConsent().copy(status = ConsentStatus.Revoked)
        val repository = FakeConsentDetailRepository(ScreenState.Content(revoked, DataFreshness.FRESH))

        val vm = viewModel(repository = repository)

        assertEquals(ConsentStatus.Revoked, content(vm).active.first().status)
    }

    // endregion

    // region — reconfirmation window

    @Test
    fun aConsentComfortablyInDateIsNotFlagged() {
        val vm = viewModel(repository = contentRepository())

        assertFalse(content(vm).active.first().isNearExpiry)
        assertFalse(content(vm).showReconfirmBanner)
    }

    @Test
    fun aConsentInsideTheWindowRaisesTheBanner() {
        val vm = viewModel(repository = FakeConsentDetailRepository(ConsentListFixtures.nearExpiryStreamState()))

        assertTrue(content(vm).active.first().isNearExpiry)
        assertTrue(content(vm).showReconfirmBanner)
    }

    @Test
    fun anExpiredConsentRaisesTheBannerToPromptRenewal() {
        val vm = viewModel(repository = FakeConsentDetailRepository(ConsentListFixtures.expiredStreamState()))

        assertTrue(content(vm).showReconfirmBanner)
    }

    // endregion

    // region — expiry arithmetic

    @Test
    fun daysUntilExpiryCountsWholeDaysForward() {
        val days = daysUntilExpiry("2026-09-26T00:00:00Z", ConsentListFixtures.NOW)

        assertEquals(ConsentListFixtures.ACTIVE_DAYS, days)
    }

    @Test
    fun daysUntilExpiryClampsAPastDateToZero() {
        val days = daysUntilExpiry("2026-01-01T00:00:00Z", ConsentListFixtures.NOW)

        assertEquals(0, days)
    }

    @Test
    fun anUnparseableExpiryIsTreatedAsZeroDaysRemaining() {
        val days = daysUntilExpiry("not-a-timestamp", ConsentListFixtures.NOW)

        assertEquals(0, days)
    }

    @Test
    fun formatConsentDateRendersDayAbbreviatedMonthAndYear() {
        assertEquals("28 Jun 2026", formatConsentDate("2026-06-28T18:25:00Z"))
    }

    @Test
    fun formatConsentDateFallsBackToTheRawValueWhenUnparseable() {
        assertEquals("whenever", formatConsentDate("whenever"))
    }

    // endregion

    // region — error classification

    @Test
    fun aTransportFaultIsTheGenericNetworkError() {
        val vm = viewModel(repository = errorRepository(NetworkError.Network(IllegalStateException("offline"))))

        assertEquals(
            ConsentListErrorKind.NetworkError,
            assertIs<ConsentListUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun aServerFaultIsTheGenericServerError() {
        val vm = viewModel(repository = errorRepository(NetworkError.Server(HTTP_INTERNAL_ERROR)))

        assertEquals(
            ConsentListErrorKind.ServerError,
            assertIs<ConsentListUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun anUnauthorisedFaultRoutesToTheDedicatedAuthStateNotTheGenericOne() {
        val vm = viewModel(repository = errorRepository(NetworkError.Client.Unauthorized()))

        assertIs<ConsentListUiState.ErrorAuth>(vm.stateFlow.value.uiState)
    }

    @Test
    fun unauthenticatedRoutesToTheDedicatedAuthState() {
        val vm = viewModel(repository = FakeConsentDetailRepository(ScreenState.Unauthenticated))

        assertIs<ConsentListUiState.ErrorAuth>(vm.stateFlow.value.uiState)
    }

    @Test
    fun noNetworkIsTheGenericNetworkError() {
        val vm = viewModel(repository = FakeConsentDetailRepository(ScreenState.NoNetwork()))

        assertEquals(
            ConsentListErrorKind.NetworkError,
            assertIs<ConsentListUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun theClassifierReturnsNullForUnauthorisedSoItCannotRenderARetryButton() {
        assertNull(classifyConsentListError(RemoteException(NetworkError.Client.Unauthorized())))
    }

    @Test
    fun anUncategorisedFaultFallsThroughToNetworkError() {
        assertEquals(ConsentListErrorKind.NetworkError, classifyConsentListError(IllegalStateException("boom")))
    }

    // endregion

    // region — retry

    @Test
    fun retryRefreshesTheStream() = runTest {
        val repository = errorRepository(NetworkError.Network(IllegalStateException("offline")))
        val vm = viewModel(repository = repository)

        vm.trySendAction(ConsentListAction.RetryLoad)
        advanceUntilIdle()

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun aLaterEmissionReplacesTheRenderedState() = runTest {
        val repository = errorRepository(NetworkError.Network(IllegalStateException("offline")))
        val vm = viewModel(repository = repository)

        repository.emit(ConsentListFixtures.contentStreamState())
        advanceUntilIdle()

        assertEquals(ConsentListFixtures.ACTIVE_ID, content(vm).active.single().consentId)
    }

    // endregion

    private companion object {
        const val HTTP_INTERNAL_ERROR = 500
    }
}
