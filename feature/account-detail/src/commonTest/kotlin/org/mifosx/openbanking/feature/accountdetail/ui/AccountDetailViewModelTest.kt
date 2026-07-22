/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.model.banking.AccountDetailWithBalances
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
import org.mifosx.openbanking.feature.accountdetail.AccountDetailChip
import org.mifosx.openbanking.feature.accountdetail.AccountDetailFixtures
import org.mifosx.openbanking.feature.accountdetail.AccountDetailRoute
import org.mifosx.openbanking.feature.accountdetail.FakeAccountCapabilityRegistry
import org.mifosx.openbanking.feature.accountdetail.FakeAccountDetailRepository
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Drives [AccountDetailViewModel] over every [ScreenState] the repository can emit and asserts the
 * display strings it produces. The repository is a hand-written fake whose stream the test pushes
 * states into.
 */
class AccountDetailViewModelTest {

    private val repo = FakeAccountDetailRepository()
    private val capabilityRegistry = FakeAccountCapabilityRegistry()

    private fun viewModel(
        handle: SavedStateHandle = SavedStateHandle(mapOf("accountId" to "acc-1")),
    ): AccountDetailViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return AccountDetailViewModel(handle, repo, capabilityRegistry)
    }

    private fun content(data: AccountDetailWithBalances): ScreenState<AccountDetailWithBalances> =
        ScreenState.Content(data = data, freshness = DataFreshness.FRESH)

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoadingAndSubscribesToTheRepositoryStream() = runTest {
        val vm = viewModel()

        assertIs<AccountDetailUiState.Loading>(vm.stateFlow.value.uiState)
        assertEquals(1, repo.stateCallCount)
    }

    @Test
    fun accountIdIsReadFromSavedStateHandleAndHandedToTheRepository() = runTest {
        val vm = viewModel(SavedStateHandle(mapOf("accountId" to "acc-42")))

        assertEquals("acc-42", vm.stateFlow.value.accountId)
        assertEquals("acc-42", repo.observedAccountId)
    }

    @Test
    fun missingAccountIdArgumentFallsBackToAnEmptyString() = runTest {
        val vm = viewModel(SavedStateHandle())

        assertEquals("", vm.stateFlow.value.accountId)
    }

    @Test
    fun contentWithBalancesRendersFormattedHeaderAndRows() = runTest {
        val vm = viewModel()

        repo.emit(content(AccountDetailFixtures.withBalances()))
        advanceUntilIdle()

        val state = assertIs<AccountDetailUiState.Content>(vm.stateFlow.value.uiState)
        assertEquals("Everyday Current", state.header.nickname)
        assertEquals("CURRENTACCOUNT", state.header.accountSubType)
        assertEquals("40-05-15 12345678", state.header.identificationLabel)
        assertEquals("GBP", state.header.currency)
        assertEquals("MIDLGB2105V", state.header.servicerIdentification)
        assertEquals("28 Jun 2026, 18:30 UTC", state.header.lastUpdatedLabel)
        assertEquals(2, state.balances.size)
        assertEquals("InterimAvailable", state.balances[0].type)
        assertEquals("2,847.63 GBP", state.balances[0].amountLabel)
        assertEquals("InterimBooked", state.balances[1].type)
        assertEquals("2,905.10 GBP", state.balances[1].amountLabel)
    }

    @Test
    fun contentWithNoBalancesRendersEmptyButKeepsTheHeader() = runTest {
        val vm = viewModel()

        repo.emit(content(AccountDetailFixtures.withBalances(balances = emptyList())))
        advanceUntilIdle()

        val state = assertIs<AccountDetailUiState.Empty>(vm.stateFlow.value.uiState)
        assertEquals("Everyday Current", state.header.nickname)
        assertEquals("40-05-15 12345678", state.header.identificationLabel)
    }

    @Test
    fun errorStateIsClassifiedAndCarriesRecoverability() = runTest {
        val vm = viewModel()

        repo.emit(ScreenState.Error(RemoteException(NetworkError.Client.Unauthorized())))
        advanceUntilIdle()

        val state = assertIs<AccountDetailUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(AccountDetailErrorKind.TokenExpired, state.kind)
        assertTrue(state.recoverable)
    }

    @Test
    fun forbiddenErrorMapsToNonRecoverableConsentWithdrawn() = runTest {
        val vm = viewModel()

        repo.emit(ScreenState.Error(RemoteException(NetworkError.Client.Forbidden())))
        advanceUntilIdle()

        val state = assertIs<AccountDetailUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(AccountDetailErrorKind.ConsentWithdrawn, state.kind)
        assertEquals(false, state.recoverable)
    }

    @Test
    fun unknownThrowableMapsToUnexpected() = runTest {
        val vm = viewModel()

        repo.emit(ScreenState.Error(IllegalStateException("mapper blew up")))
        advanceUntilIdle()

        val state = assertIs<AccountDetailUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(AccountDetailErrorKind.Unexpected, state.kind)
        assertTrue(state.recoverable)
    }

    @Test
    fun noNetworkMapsToTheNetworkErrorKind() = runTest {
        val vm = viewModel()

        repo.emit(ScreenState.NoNetwork())
        advanceUntilIdle()

        val state = assertIs<AccountDetailUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(AccountDetailErrorKind.Network, state.kind)
        assertTrue(state.recoverable)
    }

    @Test
    fun unauthenticatedMapsToTokenExpired() = runTest {
        val vm = viewModel()

        repo.emit(ScreenState.Unauthenticated)
        advanceUntilIdle()

        val state = assertIs<AccountDetailUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(AccountDetailErrorKind.TokenExpired, state.kind)
    }

    @Test
    fun screenStateLoadingRendersLoading() = runTest {
        val vm = viewModel()

        repo.emit(content(AccountDetailFixtures.withBalances()))
        advanceUntilIdle()
        repo.emit(ScreenState.Loading)
        advanceUntilIdle()

        assertIs<AccountDetailUiState.Loading>(vm.stateFlow.value.uiState)
    }

    @Test
    fun screenStateEmptyAlsoRendersLoadingBecauseTheHeaderIsNotYetKnown() = runTest {
        val vm = viewModel()

        repo.emit(content(AccountDetailFixtures.withBalances()))
        advanceUntilIdle()
        repo.emit(ScreenState.Empty)
        advanceUntilIdle()

        assertIs<AccountDetailUiState.Loading>(vm.stateFlow.value.uiState)
    }

    @Test
    fun retryLoadActionRefreshesTheRepository() = runTest {
        val vm = viewModel()
        repo.emit(ScreenState.Error(RemoteException(NetworkError.Client.Unauthorized())))
        advanceUntilIdle()

        vm.trySendAction(AccountDetailAction.RetryLoad)
        advanceUntilIdle()

        assertEquals(1, repo.refreshCount)

        vm.trySendAction(AccountDetailAction.RetryLoad)
        advanceUntilIdle()

        assertEquals(2, repo.refreshCount)
    }

    @Test
    fun aServicerlessAccountWithoutAStatusTimestampRendersBlankHeaderFields() = runTest {
        val vm = viewModel()

        repo.emit(
            content(
                AccountDetailFixtures.withBalances(
                    detail = AccountDetailFixtures.detail(
                        servicerIdentification = "",
                        statusUpdateDateTime = "",
                        sortCode = "",
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        val state = assertIs<AccountDetailUiState.Content>(vm.stateFlow.value.uiState)
        assertEquals("", state.header.servicerIdentification)
        assertEquals("", state.header.lastUpdatedLabel)
        assertEquals("12345678", state.header.identificationLabel)
    }

    @Test
    fun theRoutePropertyNameIsTheArgumentKeyTheViewModelReads() {
        val descriptor = AccountDetailRoute.serializer().descriptor

        assertEquals(1, descriptor.elementsCount)
        assertEquals(AccountDetailViewModel.ACCOUNT_ID_ARG, descriptor.getElementName(0))
    }

    @Test
    fun groupThousandsSeparatesWholeUnitsAndKeepsTheFractionExactly() {
        assertEquals("0", groupThousands("0"))
        assertEquals("123", groupThousands("123"))
        assertEquals("1,000", groupThousands("1000"))
        assertEquals("1,234,567", groupThousands("1234567"))
        assertEquals("2,847.63", groupThousands("2847.63"))
        assertEquals("1,000.005", groupThousands("1000.005"))
        assertEquals("999.00", groupThousands("999.00"))
    }

    @Test
    fun groupThousandsKeepsTheSignOnNegativeAmounts() {
        assertEquals("-2,847.63", groupThousands("-2847.63"))
        assertEquals("-42", groupThousands("-42"))
    }

    @Test
    fun groupThousandsReturnsAnythingItCannotParseUnchanged() {
        assertEquals("n/a", groupThousands("n/a"))
        assertEquals("12a4", groupThousands("12a4"))
        assertEquals(".50", groupThousands(".50"))
        assertEquals("-", groupThousands("-"))
        assertEquals("", groupThousands(""))
    }

    @Test
    fun formatStatusTimestampRendersAnIsoInstantAndTrimsTheLeadingZeroDay() {
        assertEquals("28 Jun 2026, 18:30 UTC", formatStatusTimestamp("2026-06-28T18:30:00Z"))
        assertEquals("31 Dec 2026, 23:59 UTC", formatStatusTimestamp("2026-12-31T23:59:59Z"))
        assertEquals("1 Jan 2026, 00:00 UTC", formatStatusTimestamp("2026-01-01T00:00:00Z"))
        assertEquals("8 Jun 2026, 18:30 UTC", formatStatusTimestamp("2026-06-08T18:30:00Z"))
    }

    @Test
    fun formatStatusTimestampReturnsUnparseableInputUnchanged() {
        assertEquals("not-a-date", formatStatusTimestamp("not-a-date"))
        assertEquals("", formatStatusTimestamp(""))
        assertEquals("2026-06T18:30:00Z", formatStatusTimestamp("2026-06T18:30:00Z"))
        assertEquals("2026-06-28", formatStatusTimestamp("2026-06-28"))
        assertEquals("2026-13-28T18:30:00Z", formatStatusTimestamp("2026-13-28T18:30:00Z"))
        assertEquals("2026-00-28T18:30:00Z", formatStatusTimestamp("2026-00-28T18:30:00Z"))
        assertEquals("2026-xx-28T18:30:00Z", formatStatusTimestamp("2026-xx-28T18:30:00Z"))
        assertEquals("2026-06-28T18:3", formatStatusTimestamp("2026-06-28T18:3"))
    }

    @Test
    fun buildIdentificationLabelFallsBackToWhicheverHalfIsPresent() {
        assertEquals("40-05-15 12345678", buildIdentificationLabel("400515", "12345678"))
        assertEquals("40-05-15", buildIdentificationLabel("400515", ""))
        assertEquals("12345678", buildIdentificationLabel("", "12345678"))
        assertEquals("", buildIdentificationLabel("", ""))
        assertEquals("", buildIdentificationLabel("   ", "   "))
        assertEquals("4005 1234", buildIdentificationLabel("4005", "1234"))
    }

    @Test
    fun classifyAccountDetailErrorMapsEachRemoteFailureToItsKind() {
        assertEquals(
            AccountDetailErrorKind.TokenExpired,
            classifyAccountDetailError(RemoteException(NetworkError.Client.Unauthorized())),
        )
        assertEquals(
            AccountDetailErrorKind.ConsentWithdrawn,
            classifyAccountDetailError(RemoteException(NetworkError.Client.Forbidden())),
        )
        assertEquals(
            AccountDetailErrorKind.AccountNotFound,
            classifyAccountDetailError(RemoteException(NetworkError.Client.NotFound())),
        )
        assertEquals(
            AccountDetailErrorKind.Network,
            classifyAccountDetailError(RemoteException(NetworkError.Network(cause = RuntimeException("offline")))),
        )
    }

    @Test
    fun classifyAccountDetailErrorFallsThroughToUnexpected() {
        assertEquals(
            AccountDetailErrorKind.Unexpected,
            classifyAccountDetailError(RemoteException(NetworkError.Client.RateLimited())),
        )
        assertEquals(AccountDetailErrorKind.Unexpected, classifyAccountDetailError(IllegalStateException("boom")))
        assertEquals(AccountDetailErrorKind.Unexpected, classifyAccountDetailError(NoSuchElementException()))
    }

    @Test
    fun onlyTheTwoTerminalFailuresAreUnrecoverable() {
        assertTrue(AccountDetailErrorKind.TokenExpired.recoverable)
        assertEquals(false, AccountDetailErrorKind.ConsentWithdrawn.recoverable)
        assertEquals(false, AccountDetailErrorKind.AccountNotFound.recoverable)
        assertTrue(AccountDetailErrorKind.Network.recoverable)
        assertTrue(AccountDetailErrorKind.Unexpected.recoverable)
        assertEquals(3, AccountDetailErrorKind.entries.count { it.recoverable })
    }

    // --- capability gating --------------------------------------------------------------------

    /** Standing orders and direct debits are hidden on every product except a personal current account. */
    private val standingOrderChips = setOf(AccountDetailChip.StandingOrders, AccountDetailChip.DirectDebits)

    /** The three product-gated chips: standing orders, direct debits and the credit-card-only statements. */
    private val gatedChips = standingOrderChips + AccountDetailChip.Statements

    private suspend fun chipsFor(detail: AccountDetail): Set<AccountDetailChip> {
        val vm = viewModel()
        repo.emit(content(AccountDetailFixtures.withBalances(detail = detail)))
        return vm.stateFlow.value.availableChips
    }

    /** A personal current account keeps standing orders and direct debits, but loses statements. */
    @Test
    fun aPersonalCurrentAccountKeepsEverythingExceptStatements() = runTest {
        val chips = chipsFor(AccountDetailFixtures.detail(accountTypeCode = "CACC"))

        assertEquals(AccountDetailChip.entries.toSet() - AccountDetailChip.Statements, chips)
    }

    @Test
    fun standingOrdersDirectDebitsAndStatementsAreHiddenForASavingsAccount() = runTest {
        val chips = chipsFor(
            AccountDetailFixtures.detail(
                accountSubType = "Savings",
                accountTypeCode = "SVGS",
                description = "BMM ACCOUNT",
            ),
        )

        assertEquals(AccountDetailChip.entries.toSet() - gatedChips, chips)
    }

    /** A credit card is the one product that keeps statements, though it still loses SO and DD. */
    @Test
    fun aCreditCardHidesStandingOrdersAndDirectDebitsButKeepsStatements() = runTest {
        val chips = chipsFor(
            AccountDetailFixtures.detail(
                accountSubType = "CreditCard",
                accountTypeCode = "CARD",
                description = "",
            ),
        )

        assertEquals(AccountDetailChip.entries.toSet() - standingOrderChips, chips)
        assertTrue(AccountDetailChip.Statements in chips)
    }

    /**
     * Pins sandbox account `1123456843`. It reports `CACC` exactly like a working current account,
     * so only the description tells them apart — if the resolver ever consults the type code first
     * this test is what fails.
     */
    @Test
    fun standingOrdersDirectDebitsAndStatementsAreHiddenForAGlobalMoneyAccountReportingCacc() = runTest {
        val chips = chipsFor(
            AccountDetailFixtures.detail(
                accountSubType = "",
                accountTypeCode = "CACC",
                description = "GLOBAL MONEY ACCOUNT",
            ),
        )

        assertEquals(AccountDetailChip.entries.toSet() - gatedChips, chips)
    }

    /** The runtime half: a refusal removes a chip the matrix was happy to show. */
    @Test
    fun aRecordedU000RemovesTheChipEvenWhenTheMatrixPredictedSupport() = runTest {
        val vm = viewModel()
        repo.emit(content(AccountDetailFixtures.withBalances()))
        assertTrue(AccountDetailChip.StandingOrders in vm.stateFlow.value.availableChips)

        capabilityRegistry.markUnsupported("acc-1", AccountEndpoint.StandingOrders)

        val chips = vm.stateFlow.value.availableChips
        assertEquals(false, AccountDetailChip.StandingOrders in chips)
        assertTrue(AccountDetailChip.DirectDebits in chips, "only the refused endpoint goes")
    }

    /** Only the three gated destinations are ever hideable; the other six are unconditional. */
    @Test
    fun theUngatedChipsAreNeverHiddenForAnyProductOrRefusal() {
        HsbcProductType.entries.forEach { product ->
            val chips = availableChipsFor(product, AccountEndpoint.entries.toSet())
            assertEquals(
                AccountDetailChip.entries.toSet() - gatedChips,
                chips,
                "the ungated chips must survive every product and every refusal, but did not for $product",
            )
        }
    }

    @Test
    fun aCreditCardKeepsStatements() {
        assertTrue(AccountDetailChip.Statements in availableChipsFor(HsbcProductType.CreditCard, emptySet()))
    }

    @Test
    fun aPersonalCurrentAccountHidesStatements() {
        val chips = availableChipsFor(HsbcProductType.PersonalCurrentAccount, emptySet())
        assertEquals(false, AccountDetailChip.Statements in chips)
    }

    @Test
    fun availableChipsForKeepsEverythingWhenTheProductIsUnknown() {
        assertEquals(
            AccountDetailChip.entries.toSet(),
            availableChipsFor(HsbcProductType.Unknown, emptySet()),
        )
    }
}
