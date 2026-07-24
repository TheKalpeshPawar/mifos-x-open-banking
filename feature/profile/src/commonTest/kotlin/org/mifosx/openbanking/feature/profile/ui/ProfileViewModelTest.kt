/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.user.AppLogout
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.model.hsbcPermission.OBPermission
import org.mifosx.openbanking.feature.profile.FakeConsentSession
import org.mifosx.openbanking.feature.profile.FakeProfileRepository
import org.mifosx.openbanking.feature.profile.ProfileFixtures
import org.mifosx.openbanking.feature.profile.ProfileRoute
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Covers [ProfileViewModel]'s state mapping, consent derivation, expiry arithmetic and error
 * classification.
 *
 * Test names are camelCase, not backticked: this source set also compiles for Kotlin/Native, whose
 * frontend rejects the parentheses and commas a prose-style backticked name would carry.
 */
@OptIn(ExperimentalTime::class)
class ProfileViewModelTest {

    private fun viewModel(
        repository: FakeProfileRepository = FakeProfileRepository(),
        session: FakeConsentSession = FakeConsentSession(),
        appLogout: FakeAppLogout = FakeAppLogout(),
        accountId: String = ProfileFixtures.ACCOUNT_ID,
    ): ProfileViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return ProfileViewModel(
            savedStateHandle = SavedStateHandle(mapOf("accountId" to accountId)),
            repository = repository,
            consentSession = session,
            appLogout = appLogout,
        )
    }

    private fun content(profile: PartyProfile): ScreenState<PartyProfile> =
        ScreenState.Content(data = profile, freshness = DataFreshness.FRESH)

    private fun remoteFailure(error: NetworkError): ScreenState<PartyProfile> =
        ScreenState.Error(RemoteException(error))

    private fun contentViewModel(
        session: FakeConsentSession = FakeConsentSession(),
        profile: PartyProfile = ProfileFixtures.priya,
    ): ProfileViewModel = viewModel(FakeProfileRepository(content(profile)), session)

    private fun rendered(vm: ProfileViewModel): ProfileUiState.Content =
        assertIs(vm.stateFlow.value.uiState)

    /** Expiry far enough out that nothing rounds into the warning window. */
    private fun expiresIn(days: Int): FakeConsentSession =
        FakeConsentSession(expiration = Clock.System.now() + days.days + HALF_DAY)

    // ── Route argument ───────────────────────────────────────────────────

    @Test
    fun accountIdIsReadFromSavedStateHandleUnderTheRoutePropertyName() {
        assertEquals("acc-77", viewModel(accountId = "acc-77").stateFlow.value.accountId)
    }

    @Test
    fun missingAccountIdFallsBackToEmptyRatherThanCrashing() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val vm = ProfileViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeProfileRepository(),
            consentSession = FakeConsentSession(),
            appLogout = FakeAppLogout(),
        )
        assertEquals("", vm.stateFlow.value.accountId)
    }

    /**
     * Reads the route's own serial descriptor rather than comparing the constant to a string
     * literal. A literal assertion still passes after the route property is renamed, which is
     * exactly the rename that silently breaks the `SavedStateHandle` lookup.
     */
    @Test
    fun theRouteSerialDescriptorNamesTheAccountIdArgument() {
        val descriptor = ProfileRoute.serializer().descriptor
        assertEquals(1, descriptor.elementsCount)
        assertEquals(ProfileViewModel.ACCOUNT_ID_ARG, descriptor.getElementName(0))
    }

    @Test
    fun repositoryReceivesTheRouteAccountId() {
        val repository = FakeProfileRepository()
        viewModel(repository, accountId = "acc-9")
        assertEquals("acc-9", repository.observedAccountId)
    }

    // ── State mapping ────────────────────────────────────────────────────

    @Test
    fun initialStateIsLoading() {
        assertIs<ProfileUiState.Loading>(viewModel().stateFlow.value.uiState)
    }

    @Test
    fun streamTransitionsFromLoadingToContentAsTheFetchLands() {
        val repository = FakeProfileRepository()
        val vm = viewModel(repository)
        assertIs<ProfileUiState.Loading>(vm.stateFlow.value.uiState)

        repository.emit(content(ProfileFixtures.priya))

        assertIs<ProfileUiState.Content>(vm.stateFlow.value.uiState)
    }

    @Test
    fun contentStateCarriesTheIdentityRowsTheBankSent() {
        val profile = rendered(contentViewModel()).profile

        assertEquals("Priya Sharma", profile.displayName)
        assertEquals("Personal Account Holder", profile.roleLabel)
        assertEquals("PS", profile.initials)
        assertEquals("priya.sharma@example.co.uk", profile.email)
        assertEquals("+44 7700 900482", profile.mobile)
    }

    @Test
    fun anAbsentAddressStaysBlankRatherThanBecomingAPlaceholder() {
        val profile = rendered(
            contentViewModel(profile = ProfileFixtures.priyaWithoutAddress),
        ).profile

        assertEquals("", profile.addressLine)
        assertEquals("priya.sharma@example.co.uk", profile.email)
    }

    @Test
    fun contentCarryingNoNameRendersTheEmptyState() {
        val repository = FakeProfileRepository(content(ProfileFixtures.nameless))
        assertIs<ProfileUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun emptyStreamStateRendersTheEmptyState() {
        val repository = FakeProfileRepository(ScreenState.Empty)
        assertIs<ProfileUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    // ── Sign-out ─────────────────────────────────────────────────────────

    @Test
    fun requestSignOutRaisesTheConfirmationWithoutLeavingContent() {
        val vm = contentViewModel()

        vm.trySendAction(ProfileAction.RequestSignOut)

        assertTrue(vm.stateFlow.value.isConfirmingSignOut)
        assertIs<ProfileUiState.Content>(vm.stateFlow.value.uiState)
    }

    @Test
    fun dismissSignOutClearsTheConfirmation() {
        val vm = contentViewModel()
        vm.trySendAction(ProfileAction.RequestSignOut)

        vm.trySendAction(ProfileAction.DismissSignOut)

        assertFalse(vm.stateFlow.value.isConfirmingSignOut)
    }

    @Test
    fun dismissSignOutDoesNotLogOut() {
        val appLogout = FakeAppLogout()
        val vm = viewModel(FakeProfileRepository(content(ProfileFixtures.priya)), appLogout = appLogout)
        vm.trySendAction(ProfileAction.RequestSignOut)

        vm.trySendAction(ProfileAction.DismissSignOut)

        assertEquals(0, appLogout.logOutCount)
    }

    @Test
    fun confirmSignOutLogsOutAndLowersTheDialog() {
        val appLogout = FakeAppLogout()
        val vm = viewModel(FakeProfileRepository(content(ProfileFixtures.priya)), appLogout = appLogout)
        vm.trySendAction(ProfileAction.RequestSignOut)

        vm.trySendAction(ProfileAction.ConfirmSignOut)

        assertEquals(1, appLogout.logOutCount)
        assertFalse(vm.stateFlow.value.isConfirmingSignOut)
    }

    /**
     * The fix: sign-out must be reachable from a failed profile. An expired consent renders the error
     * state, and that is exactly when a user needs to log out — the previous no-op left them stuck.
     */
    @Test
    fun requestSignOutFromTheErrorStateRaisesTheConfirmation() {
        val vm = viewModel(FakeProfileRepository(remoteFailure(NetworkError.Client.Unauthorized())))

        vm.trySendAction(ProfileAction.RequestSignOut)

        assertTrue(vm.stateFlow.value.isConfirmingSignOut)
        assertIs<ProfileUiState.Error>(vm.stateFlow.value.uiState)
    }

    @Test
    fun confirmSignOutFromTheErrorStateLogsOut() {
        val appLogout = FakeAppLogout()
        val vm = viewModel(
            FakeProfileRepository(remoteFailure(NetworkError.Client.Unauthorized())),
            appLogout = appLogout,
        )
        vm.trySendAction(ProfileAction.RequestSignOut)

        vm.trySendAction(ProfileAction.ConfirmSignOut)

        assertEquals(1, appLogout.logOutCount)
    }

    @Test
    fun aStreamReEmissionDoesNotDismissARaisedConfirmation() {
        val repository = FakeProfileRepository(content(ProfileFixtures.priya))
        val vm = viewModel(repository)
        vm.trySendAction(ProfileAction.RequestSignOut)

        repository.emit(
            ScreenState.Content(
                data = ProfileFixtures.priya,
                freshness = DataFreshness.STALE,
            ),
        )

        assertTrue(vm.stateFlow.value.isConfirmingSignOut)
    }

    // ── Consent status derivation ────────────────────────────────────────

    @Test
    fun anActiveSessionWithAFutureExpiryIsAuthorised() {
        val status = rendered(contentViewModel(expiresIn(FAR_FUTURE_DAYS))).connection.status
        assertEquals(ConsentStatus.Authorised, status)
    }

    @Test
    fun anInactiveSessionIsRevoked() {
        val session = FakeConsentSession(
            active = false,
            expiration = Clock.System.now() + FAR_FUTURE_DAYS.days,
        )
        assertEquals(ConsentStatus.Revoked, rendered(contentViewModel(session)).connection.status)
    }

    @Test
    fun anActiveSessionWithAPastExpiryIsExpired() {
        val session = FakeConsentSession(expiration = Clock.System.now() - FAR_FUTURE_DAYS.days)
        assertEquals(ConsentStatus.Expired, rendered(contentViewModel(session)).connection.status)
    }

    /** HSBC does not always return an expiry; absence must not read as a lapsed consent. */
    @Test
    fun anActiveSessionWithNoStoredExpiryIsAuthorised() {
        val session = FakeConsentSession(expiration = null)
        assertEquals(ConsentStatus.Authorised, rendered(contentViewModel(session)).connection.status)
    }

    @Test
    fun aConsentWithNoStoredExpiryRendersABlankExpiryLabel() {
        val session = FakeConsentSession(expiration = null)
        assertEquals("", rendered(contentViewModel(session)).connection.expiryLabel)
    }

    // ── Expiry window ────────────────────────────────────────────────────

    @Test
    fun aConsentEightDaysOutIsNotExpiring() {
        assertFalse(rendered(contentViewModel(expiresIn(EIGHT_DAYS))).isExpiring)
    }

    @Test
    fun aConsentSevenDaysOutIsExpiring() {
        assertTrue(rendered(contentViewModel(expiresIn(SEVEN_DAYS))).isExpiring)
    }

    @Test
    fun aConsentExpiringTodayIsExpiring() {
        val session = FakeConsentSession(expiration = Clock.System.now() + HALF_DAY)
        assertTrue(rendered(contentViewModel(session)).isExpiring)
    }

    @Test
    fun anAlreadyExpiredConsentIsExpiring() {
        val session = FakeConsentSession(expiration = Clock.System.now() - EIGHT_DAYS.days)
        assertTrue(rendered(contentViewModel(session)).isExpiring)
    }

    /**
     * The sentinel matters: `Int.MAX_VALUE` is what keeps a consent with no recorded expiry out of
     * the banner, where a zero would put every such session into it permanently.
     */
    @Test
    fun aConsentWithNoStoredExpiryIsNotExpiring() {
        val state = rendered(contentViewModel(FakeConsentSession(expiration = null)))

        assertFalse(state.isExpiring)
        assertEquals(Int.MAX_VALUE, state.daysRemaining)
    }

    @Test
    fun daysRemainingCountsWholeDaysToExpiry() {
        assertEquals(SEVEN_DAYS, rendered(contentViewModel(expiresIn(SEVEN_DAYS))).daysRemaining)
    }

    // ── Expiry formatting ────────────────────────────────────────────────

    @Test
    fun theExpiryLabelIsFormattedAsDayMonthYear() {
        val session = FakeConsentSession(expiration = Instant.parse("2026-09-26T12:00:00Z"))
        assertEquals(
            ProfileFixtures.EXPIRY_LABEL,
            rendered(contentViewModel(session)).connection.expiryLabel,
        )
    }

    /** Zero-padded so a column of dates does not shift a character between the 9th and the 10th. */
    @Test
    fun aSingleDigitDayIsZeroPaddedInTheExpiryLabel() {
        val session = FakeConsentSession(expiration = Instant.parse("2026-07-04T12:00:00Z"))
        assertEquals(
            ProfileFixtures.EXPIRING_LABEL,
            rendered(contentViewModel(session)).connection.expiryLabel,
        )
    }

    // ── Permissions ──────────────────────────────────────────────────────

    @Test
    fun contentListsEveryGrantedPermission() {
        assertEquals(OBPermission.ALL.size, rendered(contentViewModel()).permissions.size)
    }

    @Test
    fun permissionsAreTheFullGrantedScopeInCatalogueOrder() {
        assertEquals(
            OBPermission.ALL.map { it.id },
            rendered(contentViewModel()).permissions.map { it.id },
        )
    }

    @Test
    fun eachPermissionCarriesItsCatalogueLabel() {
        assertEquals(
            OBPermission.ALL.map { it.label },
            rendered(contentViewModel()).permissions.map { it.label },
        )
    }

    @Test
    fun anEmptyGrantedScopeYieldsNoRows() {
        assertTrue(profilePermissions(granted = emptyList()).isEmpty())
    }

    @Test
    fun theGrantedPermissionsStartCollapsed() {
        assertFalse(rendered(contentViewModel()).arePermissionsExpanded)
    }

    @Test
    fun togglePermissionsRevealsTheList() {
        val vm = contentViewModel()

        vm.trySendAction(ProfileAction.TogglePermissions)

        assertTrue(rendered(vm).arePermissionsExpanded)
    }

    @Test
    fun togglePermissionsAgainHidesTheList() {
        val vm = contentViewModel()
        vm.trySendAction(ProfileAction.TogglePermissions)

        vm.trySendAction(ProfileAction.TogglePermissions)

        assertFalse(rendered(vm).arePermissionsExpanded)
    }

    @Test
    fun anExpandedPermissionsListSurvivesAStreamReEmission() {
        val repository = FakeProfileRepository(content(ProfileFixtures.priya))
        val vm = viewModel(repository)
        vm.trySendAction(ProfileAction.TogglePermissions)

        repository.emit(
            ScreenState.Content(
                data = ProfileFixtures.priya,
                freshness = DataFreshness.STALE,
            ),
        )

        assertTrue(rendered(vm).arePermissionsExpanded)
    }

    // ── Error classification ─────────────────────────────────────────────

    @Test
    fun unauthorizedMapsToRetriableTokenExpired() {
        val repository = FakeProfileRepository(remoteFailure(NetworkError.Client.Unauthorized()))
        val error = assertIs<ProfileUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(ProfileErrorKind.TokenExpired, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun forbiddenMapsToConsentMissingPartyAndIsNotRetriable() {
        val repository = FakeProfileRepository(remoteFailure(NetworkError.Client.Forbidden()))
        val error = assertIs<ProfileUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(ProfileErrorKind.ConsentMissingParty, error.kind)
        assertFalse(error.kind.isRetriable)
    }

    @Test
    fun notFoundMapsToProfileNotFoundAndIsNotRetriable() {
        val repository = FakeProfileRepository(remoteFailure(NetworkError.Client.NotFound()))
        val error = assertIs<ProfileUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(ProfileErrorKind.ProfileNotFound, error.kind)
        assertFalse(error.kind.isRetriable)
    }

    @Test
    fun transportNetworkFailureMapsToRetriableLoadFailed() {
        val repository = FakeProfileRepository(
            remoteFailure(NetworkError.Network(IllegalStateException("offline"))),
        )
        val error = assertIs<ProfileUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(ProfileErrorKind.LoadFailed, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun uncategorisedFailureFallsBackToRetriableLoadFailed() {
        val repository = FakeProfileRepository(ScreenState.Error(IllegalStateException("boom")))
        val error = assertIs<ProfileUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(ProfileErrorKind.LoadFailed, error.kind)
    }

    @Test
    fun noNetworkStreamStateMapsToLoadFailed() {
        val repository = FakeProfileRepository(ScreenState.NoNetwork())
        val error = assertIs<ProfileUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(ProfileErrorKind.LoadFailed, error.kind)
    }

    @Test
    fun unauthenticatedStreamStateMapsToTokenExpired() {
        val repository = FakeProfileRepository(ScreenState.Unauthenticated)
        val error = assertIs<ProfileUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(ProfileErrorKind.TokenExpired, error.kind)
    }

    /** Retry is offered exactly where retrying can change the outcome. */
    @Test
    fun onlyTheReauthorisationFailuresAreNonRetriable() {
        assertEquals(
            listOf(ProfileErrorKind.ConsentMissingParty, ProfileErrorKind.ProfileNotFound),
            ProfileErrorKind.entries.filterNot { it.isRetriable },
        )
    }

    @Test
    fun retryActionRefreshesTheRepository() {
        val repository = FakeProfileRepository(remoteFailure(NetworkError.Client.Unauthorized()))
        val vm = viewModel(repository)

        vm.trySendAction(ProfileAction.RetryLoad)

        assertEquals(1, repository.refreshCount)
    }

    private companion object {
        const val SEVEN_DAYS = 7
        const val EIGHT_DAYS = 8
        const val FAR_FUTURE_DAYS = 90
        val HALF_DAY = 12.hours
    }
}

/** Records that the shared logout ran. Test source sets do not cross Gradle modules. */
private class FakeAppLogout : AppLogout {
    var logOutCount: Int = 0
        private set

    override suspend fun logOut() {
        logOutCount++
    }
}
