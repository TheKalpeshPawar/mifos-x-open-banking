/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail.ui

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
import org.mifosx.openbanking.feature.statementdetail.FakeStatementDetailRepository
import org.mifosx.openbanking.feature.statementdetail.FakeStatementFileHandler
import org.mifosx.openbanking.feature.statementdetail.FakeStatementFileRepository
import org.mifosx.openbanking.feature.statementdetail.StatementDetailFixtures
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Covers [StatementDetailViewModel]'s two-stream merge, display formatting, error classification and the
 * two-outcome PDF download flow.
 *
 * Test names are camelCase — this source set also compiles for Kotlin/Native, whose frontend rejects
 * the punctuation a prose-style backticked name would carry.
 */
class StatementDetailViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeStatementDetailRepository = FakeStatementDetailRepository(),
        fileRepository: FakeStatementFileRepository = FakeStatementFileRepository(),
        fileHandler: FakeStatementFileHandler = FakeStatementFileHandler(),
        accountId: String = StatementDetailFixtures.ACCOUNT_ID,
        statementId: String = StatementDetailFixtures.STATEMENT_ID,
    ): StatementDetailViewModel = StatementDetailViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                StatementDetailViewModel.ACCOUNT_ID_ARG to accountId,
                StatementDetailViewModel.STATEMENT_ID_ARG to statementId,
            ),
        ),
        repository = repository,
        fileRepository = fileRepository,
        fileHandler = fileHandler,
    )

    private fun contentRepository(): FakeStatementDetailRepository = FakeStatementDetailRepository(
        initialStatement = StatementDetailFixtures.statementContentStream(),
        initialTransactions = StatementDetailFixtures.transactionsContentStream(),
    )

    private fun emptyRepository(): FakeStatementDetailRepository = FakeStatementDetailRepository(
        initialStatement = ScreenState.Content(
            data = StatementDetailFixtures.emptyStatementDetail(),
            freshness = template.core.base.common.screen.DataFreshness.FRESH,
        ),
        initialTransactions = StatementDetailFixtures.emptyTransactionsStream(),
    )

    private fun errorRepository(error: NetworkError): FakeStatementDetailRepository =
        FakeStatementDetailRepository(
            initialStatement = ScreenState.Error(RemoteException(error)),
            initialTransactions = StatementDetailFixtures.transactionsContentStream(),
        )

    private fun content(vm: StatementDetailViewModel): StatementDetailUiState.Content =
        assertIs<StatementDetailUiState.Content>(vm.stateFlow.value.uiState)

    // region — routing & lifecycle

    @Test
    fun routeArgumentsAreReadFromSavedStateHandle() {
        val vm = viewModel(accountId = "acc-7", statementId = "stmt-9")
        assertEquals("acc-7", vm.stateFlow.value.accountId)
        assertEquals("stmt-9", vm.stateFlow.value.statementId)
    }

    @Test
    fun missingArgumentsFallBackToEmpty() {
        val vm = StatementDetailViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeStatementDetailRepository(),
            fileRepository = FakeStatementFileRepository(),
            fileHandler = FakeStatementFileHandler(),
        )
        assertEquals("", vm.stateFlow.value.accountId)
        assertEquals("", vm.stateFlow.value.statementId)
    }

    @Test
    fun argConstantsMatchTheRoutePropertyNames() {
        assertEquals("accountId", StatementDetailViewModel.ACCOUNT_ID_ARG)
        assertEquals("statementId", StatementDetailViewModel.STATEMENT_ID_ARG)
    }

    @Test
    fun repositoryReceivesBothRouteArguments() {
        val repository = FakeStatementDetailRepository()
        viewModel(repository, accountId = "acc-9", statementId = "stmt-3")
        assertEquals("acc-9", repository.observedAccountId)
        assertEquals("stmt-3", repository.observedStatementId)
    }

    @Test
    fun initialStateIsLoading() {
        assertIs<StatementDetailUiState.Loading>(viewModel().stateFlow.value.uiState)
    }

    @Test
    fun downloadStateStartsIdle() {
        assertEquals(DownloadState.Idle, viewModel().stateFlow.value.downloadState)
    }

    // endregion

    // region — two-stream merge (content)

    @Test
    fun bothStreamsContentRendersContent() {
        val rendered = content(viewModel(contentRepository()))
        assertEquals(StatementDetailFixtures.statementUiModel(), rendered.statement)
        assertEquals(StatementDetailFixtures.EXPECTED_TRANSACTION_COUNT, rendered.transactions.size)
    }

    @Test
    fun headerFieldsAreFormatted() {
        val model = content(viewModel(contentRepository())).statement
        assertEquals("MAY-2026-STMT", model.reference)
        assertEquals("1 May 2026 – 31 May 2026", model.periodLabel)
        assertEquals("RegularPeriodic", model.type)
        assertEquals("1 Jun 2026", model.createdDateLabel)
    }

    @Test
    fun balancesHumaniseTypeAndColourByCreditDebit() {
        val balances = content(viewModel(contentRepository())).statement.balances
        assertEquals("Opening Balance", balances.first().label)
        assertEquals("£2,610.40", balances.first().amountFormatted)
        assertEquals(StatementAmountColor.Credit, balances.first().color)
        assertEquals("Closing Balance", balances[1].label)
        assertEquals("£2,847.63", balances[1].amountFormatted)
    }

    @Test
    fun feesRenderNeutralAndInterestColoursByCreditDebit() {
        val model = content(viewModel(contentRepository())).statement
        assertEquals(StatementDetailFixtures.statementUiModel().fees, model.fees)
        assertEquals(StatementAmountColor.Neutral, model.fees.single().color)
        assertEquals("£0.00", model.fees.single().amountFormatted)
        assertEquals(StatementAmountColor.Credit, model.interest.single().color)
        assertEquals("£0.21", model.interest.single().amountFormatted)
    }

    @Test
    fun transactionRowsAreFormattedWithSignedAmountsAndDates() {
        val rows = content(viewModel(contentRepository())).transactions
        assertEquals(StatementDetailFixtures.transactionRows(), rows)
        assertEquals("+£3,200.00", rows[1].amountFormatted)
        assertEquals("-£82.50", rows.first().amountFormatted)
        assertEquals("3 May 2026", rows.first().dateLabel)
    }

    // endregion

    // region — empty

    @Test
    fun statementWithNoTransactionsRendersEmpty() {
        val empty = assertIs<StatementDetailUiState.Empty>(viewModel(emptyRepository()).stateFlow.value.uiState)
        assertEquals("MAY-2026-NEW", empty.statement.reference)
        assertEquals(2, empty.statement.balances.size)
    }

    // endregion

    // region — error classification

    @Test
    fun unauthorizedClassifiesToSessionExpired() {
        assertEquals(
            StatementDetailErrorCode.SessionExpired,
            errorCode(viewModel(errorRepository(NetworkError.Client.Unauthorized()))),
        )
    }

    @Test
    fun forbiddenClassifiesToConsentMissing() {
        assertEquals(
            StatementDetailErrorCode.ConsentMissingReadStatements,
            errorCode(viewModel(errorRepository(NetworkError.Client.Forbidden()))),
        )
    }

    @Test
    fun notFoundClassifiesToStatementNotFound() {
        assertEquals(
            StatementDetailErrorCode.StatementNotFound,
            errorCode(viewModel(errorRepository(NetworkError.Client.NotFound()))),
        )
    }

    @Test
    fun networkClassifiesToNetworkError() {
        assertEquals(
            StatementDetailErrorCode.NetworkError,
            errorCode(viewModel(errorRepository(NetworkError.Network(IllegalStateException("offline"))))),
        )
    }

    @Test
    fun serverFailureFallsBackToNetworkError() {
        assertEquals(
            StatementDetailErrorCode.NetworkError,
            errorCode(viewModel(errorRepository(NetworkError.Server(500)))),
        )
    }

    @Test
    fun noNetworkStreamMapsToNetworkError() {
        val repository = FakeStatementDetailRepository(
            initialStatement = ScreenState.NoNetwork(),
            initialTransactions = ScreenState.NoNetwork(),
        )
        assertEquals(StatementDetailErrorCode.NetworkError, errorCode(viewModel(repository)))
    }

    @Test
    fun unauthenticatedStreamMapsToSessionExpired() {
        val repository = FakeStatementDetailRepository(
            initialStatement = ScreenState.Unauthenticated,
            initialTransactions = ScreenState.Unauthenticated,
        )
        assertEquals(StatementDetailErrorCode.SessionExpired, errorCode(viewModel(repository)))
    }

    @Test
    fun classifyStatementDetailErrorMapsEachNetworkErrorDirectly() {
        assertEquals(
            StatementDetailErrorCode.SessionExpired,
            classifyStatementDetailError(RemoteException(NetworkError.Client.Unauthorized())),
        )
        assertEquals(
            StatementDetailErrorCode.ConsentMissingReadStatements,
            classifyStatementDetailError(RemoteException(NetworkError.Client.Forbidden())),
        )
        assertEquals(
            StatementDetailErrorCode.StatementNotFound,
            classifyStatementDetailError(RemoteException(NetworkError.Client.NotFound())),
        )
        assertEquals(
            StatementDetailErrorCode.NetworkError,
            classifyStatementDetailError(RemoteException(NetworkError.Network(IllegalStateException()))),
        )
        assertEquals(
            StatementDetailErrorCode.NetworkError,
            classifyStatementDetailError(IllegalStateException("x")),
        )
    }

    // endregion

    // region — retry

    @Test
    fun retryRefreshesBothStreams() {
        val repository = errorRepository(NetworkError.Client.Unauthorized())
        val vm = viewModel(repository)

        vm.trySendAction(StatementDetailAction.RetryLoad)

        assertEquals(1, repository.statementRefreshCount)
        assertEquals(1, repository.transactionRefreshCount)
    }

    // endregion

    // region — download

    @Test
    fun downloadFetchesTheRouteStatement() {
        val fileRepository = FakeStatementFileRepository()
        val vm = viewModel(contentRepository(), fileRepository = fileRepository)

        vm.trySendAction(StatementDetailAction.DownloadPdf)

        assertEquals(StatementDetailFixtures.ACCOUNT_ID, fileRepository.lastAccountId)
        assertEquals(StatementDetailFixtures.STATEMENT_ID, fileRepository.lastStatementId)
    }

    @Test
    fun downloadSuccessDeliversBytesToHandlerAndEmitsSucceeded() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val fileRepository = FakeStatementFileRepository(result = NetworkResult.Success(bytes))
        val fileHandler = FakeStatementFileHandler()
        val vm = viewModel(contentRepository(), fileRepository, fileHandler)
        val events = mutableListOf<StatementDetailEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.eventFlow.toList(events)
        }

        vm.trySendAction(StatementDetailAction.DownloadPdf)
        advanceUntilIdle()
        job.cancel()

        assertEquals(1, fileHandler.deliverCount)
        assertEquals("statement-${StatementDetailFixtures.STATEMENT_ID}.pdf", fileHandler.deliveredFileName)
        assertEquals("application/pdf", fileHandler.deliveredMimeType)
        assertEquals(bytes.toList(), fileHandler.deliveredBytes?.toList())
        assertEquals(listOf<StatementDetailEvent>(StatementDetailEvent.DownloadSucceeded), events)
    }

    @Test
    fun downloadFailureEmitsDownloadFailedWithReason() = runTest {
        val fileRepository = FakeStatementFileRepository(
            result = NetworkResult.Error(NetworkError.Client.NotFound()),
        )
        val vm = viewModel(contentRepository(), fileRepository = fileRepository)
        val events = mutableListOf<StatementDetailEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.eventFlow.toList(events)
        }

        vm.trySendAction(StatementDetailAction.DownloadPdf)
        advanceUntilIdle()
        job.cancel()

        assertEquals(
            listOf<StatementDetailEvent>(
                StatementDetailEvent.DownloadFailed(StatementDetailErrorCode.StatementFileNotFound.name),
            ),
            events,
        )
    }

    @Test
    fun downloadTogglesDownloadingThenIdle() {
        val gate = CompletableDeferred<Unit>()
        val fileRepository = FakeStatementFileRepository().apply { this.gate = gate }
        val vm = viewModel(contentRepository(), fileRepository = fileRepository)

        vm.trySendAction(StatementDetailAction.DownloadPdf)
        assertEquals(DownloadState.Downloading, vm.stateFlow.value.downloadState)

        gate.complete(Unit)
        assertEquals(DownloadState.Idle, vm.stateFlow.value.downloadState)
    }

    // endregion

    private fun errorCode(vm: StatementDetailViewModel): StatementDetailErrorCode =
        assertIs<StatementDetailUiState.Error>(vm.stateFlow.value.uiState).code
}
