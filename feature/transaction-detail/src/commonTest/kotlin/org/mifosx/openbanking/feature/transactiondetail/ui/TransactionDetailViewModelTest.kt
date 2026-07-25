/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.feature.transactiondetail.FakeTransactionDetailRepository
import org.mifosx.openbanking.feature.transactiondetail.TransactionDetailFixtures
import org.mifosx.openbanking.feature.transactiondetail.TransactionDetailRoute
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [TransactionDetailViewModel]'s argument routing, list-to-record resolution, display
 * formatting, error classification, retry and copy-to-clipboard event.
 *
 * Test names are camelCase, not backticked: this source set also compiles for Kotlin/Native, whose
 * frontend rejects the parentheses and commas a prose-style backticked name would carry.
 */
class TransactionDetailViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeTransactionDetailRepository = FakeTransactionDetailRepository(),
        transactionId: String = TransactionDetailFixtures.DEBIT_ID,
        accountId: String = TransactionDetailFixtures.ACCOUNT_ID,
    ): TransactionDetailViewModel = TransactionDetailViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                TransactionDetailViewModel.TRANSACTION_ID_ARG to transactionId,
                TransactionDetailViewModel.ACCOUNT_ID_ARG to accountId,
            ),
        ),
        repository = repository,
    )

    private fun contentRepository(): FakeTransactionDetailRepository =
        FakeTransactionDetailRepository(TransactionDetailFixtures.contentStreamState())

    private fun errorRepository(error: NetworkError): FakeTransactionDetailRepository =
        FakeTransactionDetailRepository(TransactionDetailFixtures.errorStreamState(error))

    private fun content(vm: TransactionDetailViewModel): TransactionDetailUiState.Content =
        assertIs<TransactionDetailUiState.Content>(vm.stateFlow.value.uiState)

    // region — routing & lifecycle

    @Test
    fun argsReadFromSavedStateHandle() {
        val vm = viewModel(transactionId = "tx-9", accountId = "acc-9")
        assertEquals("tx-9", vm.stateFlow.value.transactionId)
        assertEquals("acc-9", vm.stateFlow.value.accountId)
    }

    @Test
    fun missingArgsFallBackToEmptyStrings() {
        val vm = TransactionDetailViewModel(SavedStateHandle(), FakeTransactionDetailRepository())
        assertEquals("", vm.stateFlow.value.transactionId)
        assertEquals("", vm.stateFlow.value.accountId)
    }

    @Test
    fun argConstantsMatchTheRoutePropertyNames() {
        val descriptor = TransactionDetailRoute.serializer().descriptor
        assertEquals(2, descriptor.elementsCount)
        assertEquals(TransactionDetailViewModel.TRANSACTION_ID_ARG, descriptor.getElementName(0))
        assertEquals(TransactionDetailViewModel.ACCOUNT_ID_ARG, descriptor.getElementName(1))
    }

    @Test
    fun repositoryReceivesTheRouteAccountId() {
        val repository = FakeTransactionDetailRepository()
        viewModel(repository, accountId = "acc-42")
        assertEquals("acc-42", repository.observedAccountId)
    }

    @Test
    fun initialStateIsLoading() {
        assertIs<TransactionDetailUiState.Loading>(viewModel().stateFlow.value.uiState)
    }

    @Test
    fun loadingTransitionsToContentForAMatchingId() {
        val repository = FakeTransactionDetailRepository()
        val vm = viewModel(repository)
        assertIs<TransactionDetailUiState.Loading>(vm.stateFlow.value.uiState)

        repository.emit(TransactionDetailFixtures.contentStreamState())

        assertIs<TransactionDetailUiState.Content>(vm.stateFlow.value.uiState)
    }

    // endregion

    // region — content resolution & formatting

    @Test
    fun contentResolvesTheDebitRecordAndFormatsEveryField() {
        val model = content(viewModel(contentRepository())).transaction

        assertEquals("-£42.17", model.amountLabel)
        assertEquals(false, model.isCredit)
        assertEquals("GBP", model.currencyLabel)
        assertEquals("Tesco Stores", model.merchantLabel)
        assertEquals("Booked", model.status)
        assertTrue(model.isBooked)
        assertEquals("26 Jun 2026, 11:22", model.bookingDateLabel)
        assertEquals("26 Jun 2026, 11:22", model.valueDateLabel)
        assertEquals(TransactionCategory.GROCERIES, model.category)
        assertEquals("5411", model.merchantCategoryCode)
        assertEquals("£447.63", model.balanceAfterLabel)
    }

    /**
     * Regression: HSBC sets the per-transaction `Balance.CreditDebitIndicator` to the transaction's
     * own direction, not the balance's, so every debit on an account in credit arrives as `Debit`.
     * Signing the running balance on it rendered an account holding £21,530.92 as `-£21,530.92`.
     */
    @Test
    fun balanceAfterIsUnsignedEvenWhenTheBalanceIndicatorSaysDebit() {
        val overdrawnLooking = TransactionDetailFixtures.debit().copy(
            balanceAmount = "21530.92",
            balanceIsCredit = false,
        )
        val repository = FakeTransactionDetailRepository(
            ScreenState.Content(data = listOf(overdrawnLooking), freshness = DataFreshness.FRESH),
        )

        val model = content(viewModel(repository)).transaction

        assertEquals("£21,530.92", model.balanceAfterLabel)
        assertEquals("TESCO STORES 3476 LONDON", model.referenceLabel)
        assertEquals("DR · HSBC", model.bankCodeLabel)
    }

    @Test
    fun creditIsSignedPositiveAndFallsBackToNarrativeWhenMerchantAbsent() {
        val model = content(viewModel(contentRepository(), transactionId = TransactionDetailFixtures.CREDIT_ID))
            .transaction

        assertEquals("+£2,400.00", model.amountLabel)
        assertTrue(model.isCredit)
        assertEquals("SALARY JUN ACME LTD", model.merchantLabel)
        assertEquals("£2,847.63", model.balanceAfterLabel)
        assertEquals("CR · HSBC", model.bankCodeLabel)
    }

    @Test
    fun creditWithNoMerchantCategoryCodeHidesTheMccField() {
        val model = content(viewModel(contentRepository(), transactionId = TransactionDetailFixtures.CREDIT_ID))
            .transaction

        assertNull(model.merchantCategoryCode)
    }

    @Test
    fun pendingStatusIsNotBooked() {
        val model = content(viewModel(contentRepository(), transactionId = TransactionDetailFixtures.PENDING_ID))
            .transaction

        assertEquals("Pending", model.status)
        assertEquals(false, model.isBooked)
    }

    // endregion

    // region — empty resolution

    @Test
    fun anIdAbsentFromASuccessfulResultRendersEmptyNotError() {
        val vm = viewModel(contentRepository(), transactionId = "tx-does-not-exist")
        assertIs<TransactionDetailUiState.Empty>(vm.stateFlow.value.uiState)
    }

    @Test
    fun anEmptyResultRendersEmpty() {
        val repository = FakeTransactionDetailRepository(TransactionDetailFixtures.emptyStreamState())
        assertIs<TransactionDetailUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    @Test
    fun structuralEmptyStreamStateRendersEmpty() {
        val repository = FakeTransactionDetailRepository(template.core.base.common.screen.ScreenState.Empty)
        assertIs<TransactionDetailUiState.Empty>(viewModel(repository).stateFlow.value.uiState)
    }

    // endregion

    // region — error classification

    @Test
    fun tokenExpiredIsRecoverable() {
        val error = errorState(errorRepository(NetworkError.Client.Unauthorized()))
        assertEquals(TransactionDetailErrorKind.TokenExpired, error.kind)
        assertTrue(error.kind.recoverable)
    }

    @Test
    fun consentWithdrawnIsNotRecoverable() {
        val error = errorState(errorRepository(NetworkError.Client.Forbidden()))
        assertEquals(TransactionDetailErrorKind.ConsentWithdrawn, error.kind)
        assertEquals(false, error.kind.recoverable)
    }

    @Test
    fun networkErrorIsRecoverable() {
        val error = errorState(errorRepository(NetworkError.Network(IllegalStateException("offline"))))
        assertEquals(TransactionDetailErrorKind.Network, error.kind)
        assertTrue(error.kind.recoverable)
    }

    @Test
    fun unknownErrorFallsBackToUnexpected() {
        val repository = FakeTransactionDetailRepository(
            template.core.base.common.screen.ScreenState.Error(IllegalStateException("boom")),
        )
        assertEquals(TransactionDetailErrorKind.Unexpected, errorState(repository).kind)
    }

    @Test
    fun noNetworkStreamStateMapsToNetworkError() {
        val repository = FakeTransactionDetailRepository(template.core.base.common.screen.ScreenState.NoNetwork())
        assertEquals(TransactionDetailErrorKind.Network, errorState(repository).kind)
    }

    @Test
    fun unauthenticatedStreamStateMapsToTokenExpired() {
        val repository =
            FakeTransactionDetailRepository(template.core.base.common.screen.ScreenState.Unauthenticated)
        assertEquals(TransactionDetailErrorKind.TokenExpired, errorState(repository).kind)
    }

    @Test
    fun classifyMapsEachNetworkErrorDirectly() {
        assertEquals(
            TransactionDetailErrorKind.TokenExpired,
            classifyTransactionDetailError(RemoteException(NetworkError.Client.Unauthorized())),
        )
        assertEquals(
            TransactionDetailErrorKind.ConsentWithdrawn,
            classifyTransactionDetailError(RemoteException(NetworkError.Client.Forbidden())),
        )
        assertEquals(
            TransactionDetailErrorKind.Network,
            classifyTransactionDetailError(RemoteException(NetworkError.Network(IllegalStateException()))),
        )
        assertEquals(
            TransactionDetailErrorKind.Unexpected,
            classifyTransactionDetailError(IllegalStateException("x")),
        )
    }

    // endregion

    // region — actions

    @Test
    fun retryActionCallsRefresh() {
        val repository = errorRepository(NetworkError.Client.Unauthorized())
        val vm = viewModel(repository)

        vm.trySendAction(TransactionDetailAction.RetryLoad)

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun copyReferenceEmitsCopyToClipboardEvent() = runTest {
        val vm = viewModel(contentRepository())
        val events = mutableListOf<TransactionDetailEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.eventFlow.toList(events)
        }

        vm.trySendAction(TransactionDetailAction.CopyReference("TESCO STORES 3476 LONDON"))
        advanceUntilIdle()
        job.cancel()

        assertEquals(
            listOf<TransactionDetailEvent>(
                TransactionDetailEvent.CopyToClipboard("TESCO STORES 3476 LONDON"),
            ),
            events,
        )
    }

    // endregion

    private fun errorState(repository: FakeTransactionDetailRepository): TransactionDetailUiState.Error =
        assertIs<TransactionDetailUiState.Error>(viewModel(repository).stateFlow.value.uiState)
}
