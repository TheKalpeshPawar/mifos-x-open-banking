/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.feature.sendmoney.FakeAccountCapabilityRegistry
import org.mifosx.openbanking.feature.sendmoney.FakeAccountsOverviewRepository
import org.mifosx.openbanking.feature.sendmoney.FakeBeneficiariesRepository
import org.mifosx.openbanking.feature.sendmoney.FakePaymentInitiationRepository
import org.mifosx.openbanking.feature.sendmoney.SendMoneyFixtures
import template.core.base.common.screen.DataFreshness
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
 * What a saved-payee read that did not succeed becomes on screen.
 *
 * Split from [SendMoneyViewModelTest] for the same reason [SendMoneyRailTest] was: it is one
 * coherent question. Three outcomes of one stream — a list, no list, and no answer — used to
 * collapse into two, and the missing one is the case the bank actually produced.
 *
 * The failure this exists for was first-hand: HSBC answered `403` on `aisp/…/beneficiaries` for
 * every account, every time, while `accounts`, `balances` and `transactions` all returned `200` on
 * the same token. `content()` mapped every non-`Content` state to `emptyList()` and `renderForm`
 * switches only on the **accounts** stream, so the refusal reached the customer as "No saved
 * payees" — a claim about their own bank that was not true.
 */
class SendMoneyPayeeLoadTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val registry = FakeAccountCapabilityRegistry()

    private fun viewModel(
        accounts: FakeAccountsOverviewRepository = FakeAccountsOverviewRepository(),
        beneficiaries: FakeBeneficiariesRepository = FakeBeneficiariesRepository(),
    ) = SendMoneyViewModel(accounts, beneficiaries, FakePaymentInitiationRepository(), registry)

    private fun content(vm: SendMoneyViewModel): SendMoneyUiState.Content =
        assertIs<SendMoneyUiState.Content>(vm.stateFlow.value.uiState)

    private fun failing(state: ScreenState<Nothing>) = FakeBeneficiariesRepository(initial = state)

    /** The refusal is carried through as a failure rather than flattened into an empty list. */
    @Test
    fun aRefusedPayeeReadIsDistinguishedFromAnEmptyList() = runTest {
        val vm = viewModel(
            beneficiaries = failing(ScreenState.Error(RemoteException(NetworkError.Client.Forbidden(null)))),
        )

        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))

        val state = content(vm)
        assertTrue(state.payeesFailed)
        assertFalse(state.hasBeneficiaries)
        // Not the "choose a payer first" case either: a payer IS chosen, and the read failed anyway.
        assertFalse(state.payeesUnavailable)
    }

    /**
     * Losing the network mid-form is the same shape of answer and gets the same notice.
     *
     * Proves the flag is about the read not succeeding rather than about one error code.
     */
    @Test
    fun aPayeeReadThatCouldNotReachTheBankIsAlsoAFailure() = runTest {
        val vm = viewModel(beneficiaries = failing(ScreenState.NoNetwork()))

        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))

        assertTrue(content(vm).payeesFailed)
    }

    /**
     * `403` is an authorisation refusal, which `DecisionEngine` maps to `Unauthenticated` — and it
     * stays an inline payee failure rather than escalating to the full-screen `TokenExpired` that
     * the accounts stream raises for the same state.
     *
     * The evidence says the token is fine: every other AIS read succeeded on it, so "your
     * authorisation expired" would be false and its recovery — a browser round-trip — would not fix
     * it. `ErrorCategory.categorize` folds `401` and `403` together, so this state cannot tell an
     * expiry from a narrowed consent and must not assert the more alarming reading. And a real
     * expiry escalates on its own: the accounts stream runs on the same credential and `renderForm`
     * turns its `Unauthenticated` into `TokenExpired`. Escalating here would only pre-empt that by
     * taking away a form the PSU can still finish by hand.
     */
    @Test
    fun anUnauthorisedPayeeReadStaysInlineRatherThanTakingTheWholeScreen() = runTest {
        val vm = viewModel(beneficiaries = failing(ScreenState.Unauthenticated))

        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))

        val state = content(vm)
        assertTrue(state.payeesFailed)
        assertEquals(SendMoneyStep.Form, state.step)
    }

    /** And the accounts stream still escalates, so a genuine expiry is not swallowed by the above. */
    @Test
    fun anUnauthorisedAccountsReadStillTakesTheWholeScreen() = runTest {
        val vm = viewModel(accounts = FakeAccountsOverviewRepository(initial = ScreenState.Unauthenticated))

        val state = assertIs<SendMoneyUiState.Error>(vm.stateFlow.value.uiState)
        assertEquals(SendMoneyErrorKind.TokenExpired, state.kind)
    }

    /** The failure is not a dead end: the payee read can be run again on its own. */
    @Test
    fun retryingThePayeesRefreshesOnlyThatStream() = runTest {
        val accounts = FakeAccountsOverviewRepository()
        val beneficiaries =
            failing(ScreenState.Error(RemoteException(NetworkError.Client.Forbidden(null))))
        val vm = viewModel(accounts = accounts, beneficiaries = beneficiaries)
        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))

        vm.trySendAction(SendMoneyAction.RetryPayees)

        assertEquals(1, beneficiaries.refreshCount)
        // The accounts stream never failed, so nothing asked it to re-read. One button refreshing
        // both would re-fetch what worked and still leave the payee row with no way back.
        assertEquals(0, accounts.refreshCount)
    }

    /** With no payer there is no read in flight, so a retry has nothing to ask for. */
    @Test
    fun retryingThePayeesDoesNothingWithoutAPayer() = runTest {
        val beneficiaries = FakeBeneficiariesRepository()
        val vm = viewModel(beneficiaries = beneficiaries)

        vm.trySendAction(SendMoneyAction.RetryPayees)

        assertEquals(0, beneficiaries.refreshCount)
        assertFalse(content(vm).payeesFailed)
    }

    /** A list that loaded and is genuinely empty is not a failure — that is the other notice. */
    @Test
    fun anEmptyPayeeListIsNotAFailure() = runTest {
        val beneficiaries = FakeBeneficiariesRepository(
            initial = ScreenState.Content(emptyList(), DataFreshness.FRESH),
        )
        val vm = viewModel(beneficiaries = beneficiaries)

        vm.trySendAction(SendMoneyAction.SelectDebtorAccount(SendMoneyFixtures.CURRENT_ACCOUNT_ID))

        val state = content(vm)
        assertFalse(state.payeesFailed)
        assertFalse(state.hasBeneficiaries)
    }
}
