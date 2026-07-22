/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.feature.statements.FakeStatementFileHandler
import org.mifosx.openbanking.feature.statements.FakeStatementFileRepository
import org.mifosx.openbanking.feature.statements.FakeStatementsRepository
import org.mifosx.openbanking.feature.statements.StatementsFixtures
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers [StatementsViewModel]'s state mapping, display formatting, error classification and the
 * two-outcome download flow.
 *
 * Test names are camelCase, not backticked: this source set also compiles for Kotlin/Native, whose
 * frontend rejects the parentheses and commas a prose-style backticked name would carry.
 */
class StatementsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeStatementsRepository = FakeStatementsRepository(),
        fileRepository: FakeStatementFileRepository = FakeStatementFileRepository(),
        fileHandler: FakeStatementFileHandler = FakeStatementFileHandler(),
        accountId: String = StatementsFixtures.ACCOUNT_ID,
    ): StatementsViewModel = StatementsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(StatementsViewModel.ACCOUNT_ID_ARG to accountId)),
        repository = repository,
        fileRepository = fileRepository,
        fileHandler = fileHandler,
    )

    private fun contentRepository(): FakeStatementsRepository =
        FakeStatementsRepository(StatementsFixtures.contentStreamState())

    private fun content(vm: StatementsViewModel): StatementsUiState.Content =
        assertIs<StatementsUiState.Content>(vm.stateFlow.value.uiState)

    private fun errorRepository(error: NetworkError): FakeStatementsRepository =
        FakeStatementsRepository(StatementsFixtures.errorStreamState(error))

    // region — routing & lifecycle

    @Test
    fun accountIdReadFromSavedStateHandle() {
        assertEquals("acc-42", viewModel(accountId = "acc-42").stateFlow.value.accountId)
    }

    @Test
    fun missingAccountIdFallsBackToEmpty() {
        val vm = StatementsViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeStatementsRepository(),
            fileRepository = FakeStatementFileRepository(),
            fileHandler = FakeStatementFileHandler(),
        )
        assertEquals("", vm.stateFlow.value.accountId)
    }

    @Test
    fun accountIdArgConstantMatchesTheRoutePropertyName() {
        assertEquals("accountId", StatementsViewModel.ACCOUNT_ID_ARG)
    }

    @Test
    fun repositoryReceivesTheRouteAccountId() {
        val repository = FakeStatementsRepository()
        viewModel(repository, accountId = "acc-9")
        assertEquals("acc-9", repository.observedAccountId)
    }

    @Test
    fun initialStateIsLoading() {
        assertIs<StatementsUiState.Loading>(viewModel().stateFlow.value.uiState)
    }

    @Test
    fun loadingTransitionsToContent() {
        val repository = FakeStatementsRepository()
        val vm = viewModel(repository)
        assertIs<StatementsUiState.Loading>(vm.stateFlow.value.uiState)

        repository.emit(StatementsFixtures.contentStreamState())

        assertIs<StatementsUiState.Content>(vm.stateFlow.value.uiState)
    }

    // endregion

    // region — content mapping (TC-STMTS-001/006)

    @Test
    fun contentExposesSixRowsSortedDescendingByStartDate() {
        val rendered = content(viewModel(contentRepository()))

        assertEquals(StatementsFixtures.EXPECTED_ROW_COUNT, rendered.statements.size)
        assertEquals(
            listOf("May 2026", "April 2026", "March 2026", "February 2026", "January 2026", "December 2025"),
            rendered.statements.map { it.periodLabel },
        )
    }

    @Test
    fun closingBalanceFormattedPerRow() {
        val rendered = content(viewModel(contentRepository()))

        assertEquals("£2,847.63", rendered.statements.first().closingBalanceFormatted)
        assertEquals("£2,610.40", rendered.statements[1].closingBalanceFormatted)
        assertEquals("£1,502.88", rendered.statements.last().closingBalanceFormatted)
    }

    @Test
    fun periodLabelAndDatesDerived() {
        val may = content(viewModel(contentRepository())).statements.first()

        assertEquals("May 2026", may.periodLabel)
        assertEquals("1 May 2026", may.startDateFormatted)
        assertEquals("31 May 2026", may.endDateFormatted)
        assertEquals(StatementsFixtures.MAY_STATEMENT_ID, may.statementId)
        assertEquals(StatementsFixtures.MAY_STATEMENT_REFERENCE, may.statementReference)
    }

    @Test
    fun anUnparseableDateFallsBackToTheRawStringRatherThanCrashing() {
        val repository = FakeStatementsRepository(
            StatementsFixtures.errorStreamState(NetworkError.Server(500)),
        )
        // Sanity: the raw-date path is exercised by feeding a period the parser cannot read.
        repository.emit(
            template.core.base.common.screen.ScreenState.Content(
                data = listOf(
                    org.mifosx.openbanking.core.model.banking.StatementPeriod(
                        statementId = "STMT-X",
                        statementReference = "X",
                        startDateTime = "not-a-date",
                        endDateTime = "also-bad",
                        closingBalanceAmount = "10.00",
                        closingBalanceCurrency = "GBP",
                    ),
                ),
                freshness = template.core.base.common.screen.DataFreshness.FRESH,
            ),
        )
        val vm = viewModel(repository)
        val row = assertIs<StatementsUiState.Content>(vm.stateFlow.value.uiState).statements.single()

        assertEquals("not-a-date", row.periodLabel)
        assertEquals("not-a-date", row.startDateFormatted)
        assertEquals("also-bad", row.endDateFormatted)
        assertEquals("£10.00", row.closingBalanceFormatted)
    }

    // endregion

    // region — empty (TC-STMTS-004)

    @Test
    fun emptyContentRendersEmpty() {
        val repository = FakeStatementsRepository(StatementsFixtures.emptyStreamState())
        assertIs<StatementsUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun emptyStreamStateRendersEmpty() {
        val repository = FakeStatementsRepository(template.core.base.common.screen.ScreenState.Empty)
        assertIs<StatementsUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    // endregion

    // region — error classification (TC-STMTS-003/008/009)

    @Test
    fun tokenExpiredErrorIsRetriable() {
        val vm = viewModel(errorRepository(NetworkError.Client.Unauthorized()))
        val error = assertIs<StatementsUiState.Error>(vm.stateFlow.value.uiState)

        assertEquals(StatementsErrorKind.TokenExpired, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun consentScopeErrorIsRetriable() {
        val vm = viewModel(errorRepository(NetworkError.Client.Forbidden()))
        val error = assertIs<StatementsUiState.Error>(vm.stateFlow.value.uiState)

        assertEquals(StatementsErrorKind.ConsentScope, error.kind)
        assertTrue(error.kind.isRetriable)
    }

    @Test
    fun rateLimitedError() {
        val vm = viewModel(errorRepository(NetworkError.Client.RateLimited()))
        assertEquals(
            StatementsErrorKind.RateLimited,
            assertIs<StatementsUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun networkErrorClassified() {
        val vm = viewModel(errorRepository(NetworkError.Network(IllegalStateException("offline"))))
        assertEquals(
            StatementsErrorKind.NetworkError,
            assertIs<StatementsUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun serverErrorClassified() {
        val vm = viewModel(errorRepository(NetworkError.Server(500)))
        assertEquals(
            StatementsErrorKind.ServerError,
            assertIs<StatementsUiState.Error>(vm.stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun unknownErrorFallsBackToServerError() {
        val repository = FakeStatementsRepository(
            template.core.base.common.screen.ScreenState.Error(IllegalStateException("boom")),
        )
        assertEquals(
            StatementsErrorKind.ServerError,
            assertIs<StatementsUiState.Error>(viewModel(repository).stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun noNetworkStreamStateMapsToNetworkError() {
        val repository = FakeStatementsRepository(template.core.base.common.screen.ScreenState.NoNetwork())
        assertEquals(
            StatementsErrorKind.NetworkError,
            assertIs<StatementsUiState.Error>(viewModel(repository).stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun unauthenticatedStreamStateClassifiedAsTokenExpired() {
        val repository = FakeStatementsRepository(template.core.base.common.screen.ScreenState.Unauthenticated)
        assertEquals(
            StatementsErrorKind.TokenExpired,
            assertIs<StatementsUiState.Error>(viewModel(repository).stateFlow.value.uiState).kind,
        )
    }

    @Test
    fun everyStatementsErrorKindIsRetriable() {
        assertTrue(StatementsErrorKind.entries.all { it.isRetriable })
    }

    @Test
    fun classifyStatementsErrorMapsEachNetworkErrorDirectly() {
        assertEquals(
            StatementsErrorKind.TokenExpired,
            classifyStatementsError(RemoteException(NetworkError.Client.Unauthorized())),
        )
        assertEquals(
            StatementsErrorKind.ConsentScope,
            classifyStatementsError(RemoteException(NetworkError.Client.Forbidden())),
        )
        assertEquals(
            StatementsErrorKind.RateLimited,
            classifyStatementsError(RemoteException(NetworkError.Client.RateLimited())),
        )
        assertEquals(
            StatementsErrorKind.NetworkError,
            classifyStatementsError(RemoteException(NetworkError.Network(IllegalStateException()))),
        )
        assertEquals(StatementsErrorKind.ServerError, classifyStatementsError(IllegalStateException("x")))
    }

    // endregion

    // region — retry (TC-STMTS-003)

    @Test
    fun retryActionCallsRefresh() {
        val repository = errorRepository(NetworkError.Client.Unauthorized())
        val vm = viewModel(repository)

        vm.trySendAction(StatementsAction.RetryLoad)

        assertEquals(1, repository.refreshCount)
    }

    // endregion

    // region — download (TC-STMTS-007)

    @Test
    fun downloadFiresForCorrectStatementId() {
        val fileRepository = FakeStatementFileRepository()
        val vm = viewModel(contentRepository(), fileRepository = fileRepository)

        vm.trySendAction(StatementsAction.DownloadStatement(StatementsFixtures.MAY_STATEMENT_ID))

        assertEquals(StatementsFixtures.ACCOUNT_ID, fileRepository.lastAccountId)
        assertEquals(StatementsFixtures.MAY_STATEMENT_ID, fileRepository.lastStatementId)
    }

    @Test
    fun downloadSuccessDeliversBytesToHandler() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val fileRepository = FakeStatementFileRepository(result = NetworkResult.Success(bytes))
        val fileHandler = FakeStatementFileHandler()
        val vm = viewModel(contentRepository(), fileRepository, fileHandler)

        vm.trySendAction(StatementsAction.DownloadStatement(StatementsFixtures.MAY_STATEMENT_ID))

        assertEquals(1, fileHandler.deliverCount)
        assertEquals("statement-${StatementsFixtures.MAY_STATEMENT_ID}.pdf", fileHandler.deliveredFileName)
        assertEquals("application/pdf", fileHandler.deliveredMimeType)
        assertEquals(bytes.toList(), fileHandler.deliveredBytes?.toList())
    }

    @Test
    fun download501EmitsUnavailableEvent() = runTest {
        val fileRepository = FakeStatementFileRepository(
            result = NetworkResult.Error(NetworkError.Server(501)),
        )
        val vm = viewModel(contentRepository(), fileRepository = fileRepository)
        val events = mutableListOf<StatementsEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.eventFlow.toList(events)
        }

        vm.trySendAction(StatementsAction.DownloadStatement(StatementsFixtures.MAY_STATEMENT_ID))
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf<StatementsEvent>(StatementsEvent.ShowDownloadUnavailable), events)
    }

    @Test
    fun downloadOtherErrorEmitsErrorEvent() = runTest {
        val fileRepository = FakeStatementFileRepository(
            result = NetworkResult.Error(NetworkError.Server(500)),
        )
        val vm = viewModel(contentRepository(), fileRepository = fileRepository)
        val events = mutableListOf<StatementsEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.eventFlow.toList(events)
        }

        vm.trySendAction(StatementsAction.DownloadStatement(StatementsFixtures.MAY_STATEMENT_ID))
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf<StatementsEvent>(StatementsEvent.ShowDownloadError), events)
    }

    @Test
    fun downloadTogglesInProgressThenIdle() {
        val gate = CompletableDeferred<Unit>()
        val fileRepository = FakeStatementFileRepository().apply { this.gate = gate }
        val vm = viewModel(contentRepository(), fileRepository = fileRepository)
        val id = StatementsFixtures.MAY_STATEMENT_ID

        vm.trySendAction(StatementsAction.DownloadStatement(id))
        assertEquals(DownloadState.InProgress, vm.stateFlow.value.downloadState[id])

        gate.complete(Unit)
        assertEquals(DownloadState.Idle, vm.stateFlow.value.downloadState[id])
    }

    // endregion
}
