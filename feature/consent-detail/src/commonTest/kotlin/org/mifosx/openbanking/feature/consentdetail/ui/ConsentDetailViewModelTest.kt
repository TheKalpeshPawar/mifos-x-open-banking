/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.user.AppLogout
import org.mifosx.openbanking.feature.consentdetail.ConsentDetailFixtures
import org.mifosx.openbanking.feature.consentdetail.FakeConsentDetailRepository
import org.mifosx.openbanking.feature.consentdetail.humanisePermissionCode
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

/**
 * Covers [ConsentDetailViewModel]'s load mapping, the expiry warning window, and — the part that
 * matters most — the two-step revoke gate and what it does to local state on each outcome.
 *
 * Test names are camelCase — this source set also compiles for Kotlin/Native.
 */
@OptIn(ExperimentalTime::class)
class ConsentDetailViewModelTest {

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
        appLogout: FakeAppLogout = FakeAppLogout(),
        consentId: String = ConsentDetailFixtures.CONSENT_ID,
    ): ConsentDetailViewModel = ConsentDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ConsentDetailViewModel.CONSENT_ID_ARG to consentId)),
        repository = repository,
        appLogout = appLogout,
    )

    private fun contentRepository() = FakeConsentDetailRepository(ConsentDetailFixtures.contentStreamState())

    private fun content(vm: ConsentDetailViewModel): ConsentDetailUiState.Content =
        assertIs<ConsentDetailUiState.Content>(vm.stateFlow.value.uiState)

    // region — routing & load

    @Test
    fun theRouteArgumentIsReadFromSavedStateHandle() {
        val vm = viewModel(consentId = "aac-7")

        assertEquals("aac-7", vm.stateFlow.value.consentId)
    }

    @Test
    fun argConstantMatchesTheRoutePropertyName() {
        assertEquals("consentId", ConsentDetailViewModel.CONSENT_ID_ARG)
    }

    @Test
    fun theStreamIsOpenedForTheRoutedConsent() {
        val repository = contentRepository()

        viewModel(repository = repository)

        assertEquals(ConsentDetailFixtures.CONSENT_ID, repository.observedConsentId)
    }

    @Test
    fun contentCarriesEveryPermissionInTheBanksOrder() {
        val vm = viewModel(repository = contentRepository())

        assertEquals(ConsentDetailFixtures.permissions(), content(vm).consent.permissions)
    }

    @Test
    fun everyDateIsFormattedForDisplay() {
        val vm = viewModel(repository = contentRepository())

        val consent = content(vm).consent
        assertEquals("28 Jun 2026", consent.connectedDate)
        assertEquals("26 Sep 2026", consent.expiresDate)
        assertEquals("30 Mar 2026", consent.transactionFromDate)
        assertEquals("28 Jun 2026", consent.transactionToDate)
    }

    @Test
    fun aResponseWithNoConsentRecordIsEmpty() {
        val vm = viewModel(repository = FakeConsentDetailRepository(ScreenState.Empty))

        assertIs<ConsentDetailUiState.Empty>(vm.stateFlow.value.uiState)
    }

    // endregion

    // region — expiry warning window

    @Test
    fun aConsentOutsideTheWindowCarriesNoWarning() {
        val vm = viewModel(repository = contentRepository())

        assertNull(content(vm).consent.expiryWarningDays)
    }

    @Test
    fun aConsentInsideTheWindowCarriesItsRemainingDays() {
        val days = daysUntilExpiry("2026-07-02T00:00:00Z", ConsentDetailFixtures.NOW)

        assertEquals(THREE_DAYS, days)
    }

    @Test
    fun daysUntilExpiryClampsAPastDateToZero() {
        assertEquals(0, daysUntilExpiry("2026-01-01T00:00:00Z", ConsentDetailFixtures.NOW))
    }

    @Test
    fun anUnparseableExpiryIsTreatedAsZeroDaysRemaining() {
        assertEquals(0, daysUntilExpiry("not-a-timestamp", ConsentDetailFixtures.NOW))
    }

    @Test
    fun formatDetailDateFallsBackToTheRawValueWhenUnparseable() {
        assertEquals("whenever", formatDetailDate("whenever"))
    }

    // endregion

    // region — the revoke gate (revoke is a full logout)

    @Test
    fun tappingRevokeOpensTheGateAndLogsOutNothing() = runTest {
        val appLogout = FakeAppLogout()
        val vm = viewModel(repository = contentRepository(), appLogout = appLogout)

        vm.trySendAction(ConsentDetailAction.ConfirmRevoke)
        advanceUntilIdle()

        assertIs<ConsentDetailUiState.RevokeConfirm>(vm.stateFlow.value.uiState)
        assertEquals(0, appLogout.logOutCount)
    }

    @Test
    fun cancellingTheGateRestoresContentAndLogsOutNothing() = runTest {
        val appLogout = FakeAppLogout()
        val vm = viewModel(repository = contentRepository(), appLogout = appLogout)

        vm.trySendAction(ConsentDetailAction.ConfirmRevoke)
        advanceUntilIdle()
        vm.trySendAction(ConsentDetailAction.DismissRevokeConfirm)
        advanceUntilIdle()

        assertIs<ConsentDetailUiState.Content>(vm.stateFlow.value.uiState)
        assertEquals(0, appLogout.logOutCount)
    }

    @Test
    fun executeRevokeCannotBeReachedWithoutPassingTheGate() = runTest {
        val appLogout = FakeAppLogout()
        val vm = viewModel(repository = contentRepository(), appLogout = appLogout)

        vm.trySendAction(ConsentDetailAction.ExecuteRevoke)
        advanceUntilIdle()

        assertIs<ConsentDetailUiState.Content>(vm.stateFlow.value.uiState)
        assertEquals(0, appLogout.logOutCount)
    }

    @Test
    fun theScreenShowsTheRevokingStateWhileTheLogoutIsInFlight() = runTest {
        val appLogout = FakeAppLogout().apply { gate = CompletableDeferred() }
        val vm = viewModel(repository = contentRepository(), appLogout = appLogout)

        vm.trySendAction(ConsentDetailAction.ConfirmRevoke)
        advanceUntilIdle()
        vm.trySendAction(ConsentDetailAction.ExecuteRevoke)
        advanceUntilIdle()

        assertIs<ConsentDetailUiState.Revoking>(vm.stateFlow.value.uiState)
        appLogout.gate?.complete(Unit)
    }

    /**
     * Confirming a revoke runs the full logout. The teardown and its best-effort revoke live in
     * [org.mifosx.openbanking.core.data.user.AppLogout] (covered by its own test); here we pin that
     * the confirm surface reaches it exactly once, after the gate.
     */
    @Test
    fun confirmingRevokeRunsTheFullLogout() = runTest {
        val appLogout = FakeAppLogout()
        val vm = viewModel(repository = contentRepository(), appLogout = appLogout)

        vm.trySendAction(ConsentDetailAction.ConfirmRevoke)
        advanceUntilIdle()
        vm.trySendAction(ConsentDetailAction.ExecuteRevoke)
        advanceUntilIdle()

        assertEquals(1, appLogout.logOutCount)
    }

    // endregion

    // region — error classification

    @Test
    fun notFoundOnLoadIsConsentNotFound() {
        val vm = viewModel(
            repository = FakeConsentDetailRepository(
                ConsentDetailFixtures.errorStreamState(NetworkError.Client.NotFound()),
            ),
        )

        assertEquals(
            ConsentDetailErrorKind.ConsentNotFound,
            assertIs<ConsentDetailUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun forbiddenIsConsentRevoked() {
        val vm = viewModel(
            repository = FakeConsentDetailRepository(
                ConsentDetailFixtures.errorStreamState(NetworkError.Client.Forbidden()),
            ),
        )

        assertEquals(
            ConsentDetailErrorKind.ConsentRevoked,
            assertIs<ConsentDetailUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun unauthorizedIsTokenExpired() {
        val vm = viewModel(
            repository = FakeConsentDetailRepository(
                ConsentDetailFixtures.errorStreamState(NetworkError.Client.Unauthorized()),
            ),
        )

        assertEquals(
            ConsentDetailErrorKind.TokenExpired,
            assertIs<ConsentDetailUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun aServerFaultIsRevokeServerError() {
        val vm = viewModel(
            repository = FakeConsentDetailRepository(
                ConsentDetailFixtures.errorStreamState(NetworkError.Server(HTTP_INTERNAL_ERROR)),
            ),
        )

        assertEquals(
            ConsentDetailErrorKind.RevokeServerError,
            assertIs<ConsentDetailUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun noNetworkIsNetworkError() {
        val vm = viewModel(repository = FakeConsentDetailRepository(ScreenState.NoNetwork()))

        assertEquals(
            ConsentDetailErrorKind.NetworkError,
            assertIs<ConsentDetailUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun unauthenticatedIsTokenExpired() {
        val vm = viewModel(repository = FakeConsentDetailRepository(ScreenState.Unauthenticated))

        assertEquals(
            ConsentDetailErrorKind.TokenExpired,
            assertIs<ConsentDetailUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun anUncategorisedFaultFallsThroughToNetworkError() {
        assertEquals(ConsentDetailErrorKind.NetworkError, classifyConsentDetailError(IllegalStateException("x")))
    }

    // endregion

    // region — retry and permission labels

    @Test
    fun retryRefreshesTheStream() = runTest {
        val repository = FakeConsentDetailRepository(
            ConsentDetailFixtures.errorStreamState(NetworkError.Network(IllegalStateException("offline"))),
        )
        val vm = viewModel(repository = repository)

        vm.trySendAction(ConsentDetailAction.RetryLoad)
        advanceUntilIdle()

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun permissionCodesAreSplitIntoReadableWords() {
        assertEquals("Read beneficiaries detail", "ReadBeneficiariesDetail".humanisePermissionCode())
        assertEquals("Read balances", "ReadBalances".humanisePermissionCode())
    }

    @Test
    fun anEmptyPermissionCodeIsLeftAlone() {
        assertEquals("", "".humanisePermissionCode())
    }

    // endregion

    private companion object {
        const val HTTP_INTERNAL_ERROR = 500
        const val THREE_DAYS = 3
    }
}

/**
 * Records that the shared logout ran, with an optional [gate] to hold it open so the in-flight
 * `Revoking` state is observable. Test source sets do not cross Gradle modules.
 */
private class FakeAppLogout : AppLogout {
    var logOutCount: Int = 0
        private set
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun logOut() {
        logOutCount++
        gate?.await()
    }
}
