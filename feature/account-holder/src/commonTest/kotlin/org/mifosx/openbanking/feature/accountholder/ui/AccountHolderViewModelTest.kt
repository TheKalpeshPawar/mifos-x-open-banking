/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.feature.accountholder.AccountHolderFixtures
import org.mifosx.openbanking.feature.accountholder.AccountHolderRoute
import org.mifosx.openbanking.feature.accountholder.FakeAccountHolderRepository
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers [AccountHolderViewModel]'s state mapping and error classification. The screen is
 * identity-only: consent and sign-out live in Settings → Consents, not here.
 *
 * Test names are camelCase, not backticked: this source set also compiles for Kotlin/Native, whose
 * frontend rejects the parentheses and commas a prose-style backticked name would carry.
 */
class AccountHolderViewModelTest {

    private fun viewModel(
        repository: FakeAccountHolderRepository = FakeAccountHolderRepository(),
        accountId: String = AccountHolderFixtures.ACCOUNT_ID,
    ): AccountHolderViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return AccountHolderViewModel(
            savedStateHandle = SavedStateHandle(mapOf("accountId" to accountId)),
            repository = repository,
        )
    }

    private fun content(profile: PartyProfile): ScreenState<PartyProfile> =
        ScreenState.Content(data = profile, freshness = DataFreshness.FRESH)

    private fun remoteFailure(error: NetworkError): ScreenState<PartyProfile> =
        ScreenState.Error(RemoteException(error))

    private fun contentViewModel(
        profile: PartyProfile = AccountHolderFixtures.priya,
    ): AccountHolderViewModel = viewModel(FakeAccountHolderRepository(content(profile)))

    private fun rendered(vm: AccountHolderViewModel): AccountHolderUiState.Content =
        assertIs(vm.stateFlow.value.uiState)

    // ── Route argument ───────────────────────────────────────────────────

    @Test
    fun accountIdIsReadFromSavedStateHandleUnderTheRoutePropertyName() {
        assertEquals("acc-77", viewModel(accountId = "acc-77").stateFlow.value.accountId)
    }

    @Test
    fun missingAccountIdFallsBackToEmptyRatherThanCrashing() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val vm = AccountHolderViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeAccountHolderRepository(),
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
        val descriptor = AccountHolderRoute.serializer().descriptor
        assertEquals(1, descriptor.elementsCount)
        assertEquals(AccountHolderViewModel.ACCOUNT_ID_ARG, descriptor.getElementName(0))
    }

    @Test
    fun repositoryReceivesTheRouteAccountId() {
        val repository = FakeAccountHolderRepository()
        viewModel(repository, accountId = "acc-9")
        assertEquals("acc-9", repository.observedAccountId)
    }

    // ── State mapping ────────────────────────────────────────────────────

    @Test
    fun initialStateIsLoading() {
        assertIs<AccountHolderUiState.Loading>(viewModel().stateFlow.value.uiState)
    }

    @Test
    fun streamTransitionsFromLoadingToContentAsTheFetchLands() {
        val repository = FakeAccountHolderRepository()
        val vm = viewModel(repository)
        assertIs<AccountHolderUiState.Loading>(vm.stateFlow.value.uiState)

        repository.emit(content(AccountHolderFixtures.priya))

        assertIs<AccountHolderUiState.Content>(vm.stateFlow.value.uiState)
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
            contentViewModel(profile = AccountHolderFixtures.priyaWithoutAddress),
        ).profile

        assertEquals("", profile.addressLine)
        assertEquals("priya.sharma@example.co.uk", profile.email)
    }

    @Test
    fun contentCarryingNoNameRendersTheEmptyState() {
        val repository = FakeAccountHolderRepository(content(AccountHolderFixtures.nameless))
        assertIs<AccountHolderUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun emptyStreamStateRendersTheEmptyState() {
        val repository = FakeAccountHolderRepository(ScreenState.Empty)
        assertIs<AccountHolderUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    // ── Error classification ─────────────────────────────────────────────

    @Test
    fun unauthorizedMapsToRetriableTokenExpired() {
        val repository = FakeAccountHolderRepository(remoteFailure(NetworkError.Client.Unauthorized()))
        val error = assertIs<AccountHolderUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(AccountHolderErrorKind.TokenExpired, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun forbiddenMapsToConsentMissingPartyAndIsNotRetriable() {
        val repository = FakeAccountHolderRepository(remoteFailure(NetworkError.Client.Forbidden()))
        val error = assertIs<AccountHolderUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(AccountHolderErrorKind.ConsentMissingParty, error.kind)
        assertFalse(error.kind.isRetriable)
    }

    @Test
    fun notFoundMapsToProfileNotFoundAndIsNotRetriable() {
        val repository = FakeAccountHolderRepository(remoteFailure(NetworkError.Client.NotFound()))
        val error = assertIs<AccountHolderUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(AccountHolderErrorKind.ProfileNotFound, error.kind)
        assertFalse(error.kind.isRetriable)
    }

    @Test
    fun transportNetworkFailureMapsToRetriableLoadFailed() {
        val repository = FakeAccountHolderRepository(
            remoteFailure(NetworkError.Network(IllegalStateException("offline"))),
        )
        val error = assertIs<AccountHolderUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(AccountHolderErrorKind.LoadFailed, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun uncategorisedFailureFallsBackToRetriableLoadFailed() {
        val repository = FakeAccountHolderRepository(ScreenState.Error(IllegalStateException("boom")))
        val error = assertIs<AccountHolderUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(AccountHolderErrorKind.LoadFailed, error.kind)
    }

    @Test
    fun noNetworkStreamStateMapsToLoadFailed() {
        val repository = FakeAccountHolderRepository(ScreenState.NoNetwork())
        val error = assertIs<AccountHolderUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(AccountHolderErrorKind.LoadFailed, error.kind)
    }

    @Test
    fun unauthenticatedStreamStateMapsToTokenExpired() {
        val repository = FakeAccountHolderRepository(ScreenState.Unauthenticated)
        val error = assertIs<AccountHolderUiState.Error>(viewModel(repository).stateFlow.value.uiState)

        assertEquals(AccountHolderErrorKind.TokenExpired, error.kind)
    }

    /** Retry is offered exactly where retrying can change the outcome. */
    @Test
    fun onlyTheReauthorisationFailuresAreNonRetriable() {
        assertEquals(
            listOf(AccountHolderErrorKind.ConsentMissingParty, AccountHolderErrorKind.ProfileNotFound),
            AccountHolderErrorKind.entries.filterNot { it.isRetriable },
        )
    }

    @Test
    fun retryActionRefreshesTheRepository() {
        val repository = FakeAccountHolderRepository(remoteFailure(NetworkError.Client.Unauthorized()))
        val vm = viewModel(repository)

        vm.trySendAction(AccountHolderAction.RetryLoad)

        assertEquals(1, repository.refreshCount)
    }
}
