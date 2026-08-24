/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.feature.beneficiaries.BeneficiariesFixtures
import org.mifosx.openbanking.feature.beneficiaries.FakeBeneficiariesRepository
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers [BeneficiariesViewModel]'s stream mapping, display formatting, error classification and the
 * client-side search.
 *
 * Test names are camelCase — this source set also compiles for Kotlin/Native, whose frontend rejects
 * the punctuation a prose-style backticked name would carry.
 */
class BeneficiariesViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeBeneficiariesRepository = FakeBeneficiariesRepository(),
        accountId: String = BeneficiariesFixtures.ACCOUNT_ID,
    ): BeneficiariesViewModel = BeneficiariesViewModel(
        savedStateHandle = SavedStateHandle(mapOf(BeneficiariesViewModel.ACCOUNT_ID_ARG to accountId)),
        repository = repository,
    )

    private fun contentRepository() = FakeBeneficiariesRepository(BeneficiariesFixtures.contentStreamState())

    private fun emptyRepository() = FakeBeneficiariesRepository(BeneficiariesFixtures.emptyStreamState())

    private fun errorRepository(error: NetworkError) =
        FakeBeneficiariesRepository(BeneficiariesFixtures.errorStreamState(error))

    private fun content(vm: BeneficiariesViewModel): BeneficiariesUiState.Content =
        assertIs<BeneficiariesUiState.Content>(vm.stateFlow.value.uiState)

    private fun errorKind(vm: BeneficiariesViewModel): BeneficiariesErrorKind =
        assertIs<BeneficiariesUiState.Error>(vm.stateFlow.value.uiState).kind

    // region — routing & lifecycle

    @Test
    fun theRouteArgumentIsReadFromSavedStateHandle() {
        val vm = viewModel(accountId = "acc-7")

        assertEquals("acc-7", vm.stateFlow.value.accountId)
    }

    @Test
    fun aMissingArgumentFallsBackToEmpty() {
        val vm = BeneficiariesViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeBeneficiariesRepository(),
        )

        assertEquals("", vm.stateFlow.value.accountId)
    }

    @Test
    fun argConstantMatchesTheRoutePropertyName() {
        assertEquals("accountId", BeneficiariesViewModel.ACCOUNT_ID_ARG)
    }

    @Test
    fun theStreamIsOpenedForTheRoutedAccount() {
        val repository = contentRepository()

        viewModel(repository = repository)

        assertEquals(BeneficiariesFixtures.ACCOUNT_ID, repository.observedAccountId)
    }

    @Test
    fun theInitialStateIsLoading() {
        val vm = viewModel()

        assertIs<BeneficiariesUiState.Loading>(vm.stateFlow.value.uiState)
    }

    // endregion

    // region — content mapping

    @Test
    fun contentCarriesEveryPayeeInTheBanksOrder() {
        val vm = viewModel(repository = contentRepository())

        val current = content(vm)
        assertEquals(BeneficiariesFixtures.EXPECTED_COUNT, current.all.size)
        assertEquals(
            listOf("BEN-001", "BEN-002", "BEN-003", "BEN-004", "BEN-005"),
            current.all.map { it.beneficiaryId },
        )
    }

    @Test
    fun withNoQueryTheFilteredListIsTheWholeList() {
        val vm = viewModel(repository = contentRepository())

        val current = content(vm)
        assertEquals(current.all, current.filtered)
        assertEquals("", current.query)
    }

    @Test
    fun anIbanIsGroupedIntoFourCharacterBlocksForDisplay() {
        val vm = viewModel(repository = contentRepository())

        val iban = content(vm).all.first { it.beneficiaryId == BeneficiariesFixtures.IBAN_ID }
        assertEquals("DE89 3704 0044 0532 0130 00", formatIdentification(iban.identification, iban.scheme))
        assertEquals(BeneficiaryScheme.Iban, iban.scheme)
    }

    @Test
    fun aSortCodeIdentifierIsLeftExactlyAsTheBankSentIt() {
        val vm = viewModel(repository = contentRepository())

        val first = content(vm).all.first()
        assertEquals("40-12-09 65872310", first.identification)
        assertEquals(BeneficiaryScheme.SortCode, first.scheme)
    }

    @Test
    fun theReferenceIsCarriedThroughUntouched() {
        val vm = viewModel(repository = contentRepository())

        assertEquals("RENT-FLAT12", content(vm).all.first().reference)
    }

    // endregion

    // region — empty

    @Test
    fun anAccountWithNoPayeesIsEmptyNotContent() {
        val vm = viewModel(repository = emptyRepository())

        assertIs<BeneficiariesUiState.Empty>(vm.stateFlow.value.uiState)
    }

    // endregion

    // region — error classification

    @Test
    fun unauthorizedIsTokenExpired() {
        val vm = viewModel(repository = errorRepository(NetworkError.Client.Unauthorized()))

        assertEquals(BeneficiariesErrorKind.TokenExpired, errorKind(vm))
    }

    @Test
    fun forbiddenIsConsentRevoked() {
        val vm = viewModel(repository = errorRepository(NetworkError.Client.Forbidden()))

        assertEquals(BeneficiariesErrorKind.ConsentRevoked, errorKind(vm))
    }

    @Test
    fun rateLimitedIsRateLimited() {
        val vm = viewModel(repository = errorRepository(NetworkError.Client.RateLimited()))

        assertEquals(BeneficiariesErrorKind.RateLimited, errorKind(vm))
    }

    @Test
    fun aServerFaultIsServerError() {
        val vm = viewModel(repository = errorRepository(NetworkError.Server(HTTP_INTERNAL_ERROR)))

        assertEquals(BeneficiariesErrorKind.ServerError, errorKind(vm))
    }

    @Test
    fun aTransportFaultIsNetworkError() {
        val vm = viewModel(repository = errorRepository(NetworkError.Network(IllegalStateException("offline"))))

        assertEquals(BeneficiariesErrorKind.NetworkError, errorKind(vm))
    }

    @Test
    fun noNetworkIsNetworkError() {
        val repository = FakeBeneficiariesRepository(ScreenState.NoNetwork())
        val vm = viewModel(repository = repository)

        assertEquals(BeneficiariesErrorKind.NetworkError, errorKind(vm))
    }

    @Test
    fun unauthenticatedIsTokenExpired() {
        val repository = FakeBeneficiariesRepository(ScreenState.Unauthenticated)
        val vm = viewModel(repository = repository)

        assertEquals(BeneficiariesErrorKind.TokenExpired, errorKind(vm))
    }

    @Test
    fun anUncategorisedFaultFallsThroughToNetworkError() {
        assertEquals(BeneficiariesErrorKind.NetworkError, classifyBeneficiariesError(IllegalStateException("boom")))
    }

    @Test
    fun aRemoteExceptionIsUnwrappedByTheClassifier() {
        val kind = classifyBeneficiariesError(RemoteException(NetworkError.Client.Forbidden()))

        assertEquals(BeneficiariesErrorKind.ConsentRevoked, kind)
    }

    @Test
    fun onlyARevokedConsentIsNonRetriable() {
        assertFalse(BeneficiariesErrorKind.ConsentRevoked.isRetriable)
        assertTrue(BeneficiariesErrorKind.TokenExpired.isRetriable)
        assertTrue(BeneficiariesErrorKind.RateLimited.isRetriable)
        assertTrue(BeneficiariesErrorKind.NetworkError.isRetriable)
        assertTrue(BeneficiariesErrorKind.ServerError.isRetriable)
    }

    // endregion

    // region — retry

    @Test
    fun retryRefreshesTheStream() = runTest {
        val repository = errorRepository(NetworkError.Network(IllegalStateException("offline")))
        val vm = viewModel(repository = repository)

        vm.trySendAction(BeneficiariesAction.RetryLoad)
        advanceUntilIdle()

        assertEquals(1, repository.refreshCount)
    }

    // endregion

    // region — search

    @Test
    fun searchingByNameKeepsOnlyTheMatchingPayee() = runTest {
        val vm = viewModel(repository = contentRepository())

        vm.trySendAction(BeneficiariesAction.Search(BeneficiariesFixtures.NAME_QUERY))
        advanceUntilIdle()

        val current = content(vm)
        assertEquals(listOf(BeneficiariesFixtures.ENERGY_ID), current.filtered.map { it.beneficiaryId })
        assertEquals(BeneficiariesFixtures.EXPECTED_COUNT, current.all.size)
    }

    @Test
    fun searchingByReferenceMatchesCaseInsensitively() = runTest {
        val vm = viewModel(repository = contentRepository())

        vm.trySendAction(BeneficiariesAction.Search(BeneficiariesFixtures.REFERENCE_QUERY))
        advanceUntilIdle()

        assertEquals(listOf(BeneficiariesFixtures.ISA_ID), content(vm).filtered.map { it.beneficiaryId })
    }

    @Test
    fun theAccountIdentifierIsNotSearched() = runTest {
        val vm = viewModel(repository = contentRepository())

        vm.trySendAction(BeneficiariesAction.Search("65872310"))
        advanceUntilIdle()

        assertTrue(content(vm).filtered.isEmpty())
    }

    @Test
    fun clearingTheQueryRestoresEveryPayeeWithoutRefetching() = runTest {
        val repository = contentRepository()
        val vm = viewModel(repository = repository)

        vm.trySendAction(BeneficiariesAction.Search(BeneficiariesFixtures.NAME_QUERY))
        advanceUntilIdle()
        vm.trySendAction(BeneficiariesAction.Search(""))
        advanceUntilIdle()

        assertEquals(BeneficiariesFixtures.EXPECTED_COUNT, content(vm).filtered.size)
        assertEquals(0, repository.refreshCount)
    }

    @Test
    fun aQueryMatchingNothingStaysInContentRatherThanFallingIntoEmpty() = runTest {
        val vm = viewModel(repository = contentRepository())

        vm.trySendAction(BeneficiariesAction.Search(BeneficiariesFixtures.NO_MATCH_QUERY))
        advanceUntilIdle()

        val current = content(vm)
        assertTrue(current.filtered.isEmpty())
        assertTrue(current.isSearchWithoutMatches)
        assertEquals(BeneficiariesFixtures.EXPECTED_COUNT, current.all.size)
    }

    @Test
    fun aBlankQueryIsNotTreatedAsANoResultsSearch() = runTest {
        val vm = viewModel(repository = contentRepository())

        vm.trySendAction(BeneficiariesAction.Search("   "))
        advanceUntilIdle()

        val current = content(vm)
        assertEquals(BeneficiariesFixtures.EXPECTED_COUNT, current.filtered.size)
        assertFalse(current.isSearchWithoutMatches)
    }

    @Test
    fun theLiveQuerySurvivesAStreamReEmission() = runTest {
        val repository = contentRepository()
        val vm = viewModel(repository = repository)
        vm.trySendAction(BeneficiariesAction.Search(BeneficiariesFixtures.NAME_QUERY))
        advanceUntilIdle()

        repository.emit(BeneficiariesFixtures.contentStreamState())
        advanceUntilIdle()

        val current = content(vm)
        assertEquals(BeneficiariesFixtures.NAME_QUERY, current.query)
        assertEquals(listOf(BeneficiariesFixtures.ENERGY_ID), current.filtered.map { it.beneficiaryId })
    }

    @Test
    fun searchingBeforeContentArrivesIsIgnored() = runTest {
        val vm = viewModel()

        vm.trySendAction(BeneficiariesAction.Search(BeneficiariesFixtures.NAME_QUERY))
        advanceUntilIdle()

        assertIs<BeneficiariesUiState.Loading>(vm.stateFlow.value.uiState)
    }

    @Test
    fun aQueryIsTrimmedBeforeMatching() = runTest {
        val vm = viewModel(repository = contentRepository())

        vm.trySendAction(BeneficiariesAction.Search("  ener  "))
        advanceUntilIdle()

        assertEquals(listOf(BeneficiariesFixtures.ENERGY_ID), content(vm).filtered.map { it.beneficiaryId })
    }

    // endregion

    private companion object {
        const val HTTP_INTERNAL_ERROR = 500
    }
}
