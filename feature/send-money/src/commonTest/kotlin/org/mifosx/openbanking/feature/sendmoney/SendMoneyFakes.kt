/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.mifosx.openbanking.core.data.banking.AccountsOverviewRepository
import org.mifosx.openbanking.core.data.banking.BeneficiariesRepository
import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.StagedConsent
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import template.core.base.store.screen.ScreenDataStream

private const val REFRESH_REPLAY = 16

class FakeAccountsOverviewRepository(
    initial: ScreenState<List<AccountWithBalance>> = ScreenState.Content(
        SendMoneyFixtures.accounts(),
        DataFreshness.FRESH,
    ),
) : AccountsOverviewRepository {

    val states = MutableStateFlow(initial)

    var refreshCount: Int = 0
        private set

    override fun overviewState(scope: CoroutineScope): Flow<ScreenState<List<AccountWithBalance>>> = states

    override fun refresh() {
        refreshCount++
    }
}

class FakeBeneficiariesRepository(
    initial: ScreenState<List<BeneficiaryItem>> = ScreenState.Content(
        SendMoneyFixtures.beneficiaries(),
        DataFreshness.FRESH,
    ),
) : BeneficiariesRepository {

    val states = MutableStateFlow(initial)

    /** Which account ids the screen asked for, in order — proves the list re-keys on the payer. */
    val requestedAccountIds = mutableListOf<String>()

    /**
     * The trigger must be buffered: `refresh()` is a `tryEmit`, so a bare `MutableSharedFlow` drops
     * it silently and the recorded count never moves.
     */
    private val refreshes = MutableSharedFlow<Unit>(replay = REFRESH_REPLAY)

    override fun beneficiariesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<BeneficiaryItem>> {
        requestedAccountIds += accountId
        return ScreenDataStream(state = states, refreshTrigger = refreshes)
    }
}

/**
 * Records every call so the tests can assert the two properties that matter: that a retry reuses the
 * idempotency key and draft, and that a negative funds check stops the submission.
 */
class FakePaymentInitiationRepository(
    private var stageResult: NetworkResult<StagedConsent, NetworkError> =
        NetworkResult.Success(SendMoneyFixtures.stagedConsent()),
    private var fundsResult: NetworkResult<Boolean, NetworkError> = NetworkResult.Success(true),
    private var submitResult: NetworkResult<PaymentReceipt, NetworkError> =
        NetworkResult.Success(SendMoneyFixtures.receipt()),
) : PaymentInitiationRepository {

    val stagedDrafts = mutableListOf<PaymentDraft>()
    val submittedDrafts = mutableListOf<PaymentDraft>()
    val submittedConsentIds = mutableListOf<String>()
    val fundsChecks = mutableListOf<String>()

    override suspend fun stagePayment(draft: PaymentDraft): NetworkResult<StagedConsent, NetworkError> {
        stagedDrafts += draft
        return stageResult
    }

    override suspend fun confirmFunds(consentId: String): NetworkResult<Boolean, NetworkError> {
        fundsChecks += consentId
        return fundsResult
    }

    override suspend fun submitPayment(
        draft: PaymentDraft,
        consentId: String,
    ): NetworkResult<PaymentReceipt, NetworkError> {
        submittedDrafts += draft
        submittedConsentIds += consentId
        return submitResult
    }

    override suspend fun paymentStatus(
        domesticPaymentId: String,
    ): NetworkResult<PaymentReceipt, NetworkError> = NetworkResult.Success(SendMoneyFixtures.receipt())

    fun stageReturns(result: NetworkResult<StagedConsent, NetworkError>) {
        stageResult = result
    }

    fun fundsReturn(result: NetworkResult<Boolean, NetworkError>) {
        fundsResult = result
    }

    fun submitReturns(result: NetworkResult<PaymentReceipt, NetworkError>) {
        submitResult = result
    }
}
