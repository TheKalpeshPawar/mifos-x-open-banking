/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.login.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.auth.AuthRecoveryRepository
import org.mifosx.openbanking.core.model.obp.ObpException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAuthRecoveryRepository(
    var initiateResult: Result<String> = Result.success("ok"),
) : AuthRecoveryRepository {
    var lastEmail: String? = null
    override suspend fun initiateReset(email: String): Result<String> {
        lastEmail = email
        return initiateResult
    }

    override suspend fun confirmReset(token: String, newPassword: String): Result<String> =
        Result.success("ok")

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): Result<String> = Result.success("ok")
}

class ForgotPasswordViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: AuthRecoveryRepository = FakeAuthRecoveryRepository()) =
        ForgotPasswordViewModel(repo)

    @Test
    fun submitDisabledUntilIdentifierPresent() = runTest {
        val vm = viewModel()
        assertFalse(vm.stateFlow.value.isFormValid)
        vm.trySendAction(ForgotPasswordAction.IdentifierChanged("alice@example.com"))
        assertTrue(vm.stateFlow.value.isFormValid)
    }

    @Test
    fun successSwapsToConfirmation() = runTest {
        val repo = FakeAuthRecoveryRepository(initiateResult = Result.success("sent"))
        val vm = viewModel(repo)
        vm.trySendAction(ForgotPasswordAction.IdentifierChanged("alice@example.com"))
        vm.trySendAction(ForgotPasswordAction.SubmitClicked)

        val state = vm.stateFlow.value
        assertEquals("alice@example.com", repo.lastEmail)
        assertTrue(state.isSuccess)
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
    }

    @Test
    fun notFoundShowsAccountMessage() = runTest {
        val repo = FakeAuthRecoveryRepository(
            initiateResult = Result.failure(ObpException(reason = "NOT_FOUND")),
        )
        val vm = viewModel(repo)
        vm.trySendAction(ForgotPasswordAction.IdentifierChanged("ghost"))
        vm.trySendAction(ForgotPasswordAction.SubmitClicked)

        val state = vm.stateFlow.value
        assertEquals(
            "We couldn't find an account with that username or email. Please try again.",
            state.errorMessage,
        )
        assertFalse(state.isSuccess)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun rateLimitedShowsRateLimitMessage() = runTest {
        val repo = FakeAuthRecoveryRepository(
            initiateResult = Result.failure(ObpException(reason = "TOO_MANY_REQUESTS")),
        )
        val vm = viewModel(repo)
        vm.trySendAction(ForgotPasswordAction.IdentifierChanged("alice@example.com"))
        vm.trySendAction(ForgotPasswordAction.SubmitClicked)

        assertEquals(
            "Too many requests. Please wait a moment and try again.",
            vm.stateFlow.value.errorMessage,
        )
    }

    @Test
    fun backToLoginEmitsNavigationEvent() = runTest {
        val vm = viewModel()
        var navigated = false
        val job = CoroutineScope(dispatcher).launch {
            vm.eventFlow.collect { if (it is ForgotPasswordEvent.NavigateBackToLogin) navigated = true }
        }
        vm.trySendAction(ForgotPasswordAction.BackToLoginClicked)
        job.cancel()
        assertTrue(navigated)
    }
}
