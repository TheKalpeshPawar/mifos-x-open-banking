/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.cards.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.cards.CardsRepository
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.Card
import org.mifosx.openbanking.core.model.obp.Transaction
import template.core.base.store.screen.ScreenState
import template.core.base.store.screen.combineContent
import template.core.base.store.screen.emptyIfContent

/**
 * My Cards ViewModel. The carousel is the offline-first [CardsRepository.userCardsStream]
 * (GET /obp/v7.0.0/cards — every card the signed-in user holds). Once cards load, the
 * recent-transactions section is fetched for the first card's owning account
 * ([Card.account].id) via [TransactionsRepository]; that secondary list is fused into the
 * single [ScreenState] so the screen renders loading / content / empty / error uniformly.
 *
 * The card controls (freeze, set limit, view PIN, report lost, order new card) are
 * deferred: OBP exposes them only through management endpoints that require the
 * CanUpdateCardsForBank role and a CARD_ID the consumer cards payload does not return
 * (verified against the OBP sandbox, 2026-05-31). Tapping one emits a "coming soon"
 * message rather than calling a non-functional endpoint.
 */
class CardsViewModel(
    cardsRepository: CardsRepository,
    private val transactionsRepository: TransactionsRepository,
) : ViewModel() {

    private val stream = cardsRepository.userCardsStream(viewModelScope)

    private val transactionsFlow = MutableStateFlow(TransactionsState())

    private val deferredMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val deferredActionMessages: SharedFlow<String> = deferredMessages.asSharedFlow()

    val uiState: StateFlow<ScreenState<CardsContent>> = stream.state
        .combineContent(transactionsFlow) { cards, transactions, _ ->
            CardsContent(
                cards = cards,
                selectedCardId = cards.firstOrNull()?.bankCardNumber.orEmpty(),
                recentTransactions = transactions.items,
                transactionsLoading = transactions.loading,
            )
        }
        .emptyIfContent { it.cards.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    init {
        viewModelScope.launch {
            stream.state.collect { state ->
                if (state is ScreenState.Content) {
                    val account = state.data.firstOrNull()?.account
                    val accountId = account?.id.orEmpty()
                    val bankId = account?.bankId.orEmpty()
                    if (accountId.isNotBlank() && bankId.isNotBlank()) {
                        transactionsFlow.value = transactionsFlow.value.copy(loading = true)
                        transactionsRepository.listTransactions(bankId, accountId, limit = RECENT_LIMIT)
                            .onSuccess {
                                transactionsFlow.value = TransactionsState(
                                    loading = false,
                                    items = it.take(RECENT_LIMIT),
                                )
                            }
                            .onFailure {
                                transactionsFlow.value = transactionsFlow.value.copy(loading = false)
                            }
                    }
                }
            }
        }
    }

    fun onRetry() = stream.retry()

    fun onRefresh() = stream.refresh()

    /** Tapping any deferred card control surfaces a "coming soon" message. */
    fun onDeferredAction(control: String) {
        deferredMessages.tryEmit("$control is coming soon")
    }

    private companion object {
        const val RECENT_LIMIT = 5
    }
}

/** Loaded content for the My Cards screen — the card carousel + recent card transactions. */
@Immutable
data class CardsContent(
    val cards: List<Card>,
    val selectedCardId: String,
    val recentTransactions: List<Transaction>,
    val transactionsLoading: Boolean = false,
)

/** Secondary-fetch state for the recent-transactions section (loaded after the cards). */
@Immutable
private data class TransactionsState(
    val loading: Boolean = false,
    val items: List<Transaction> = emptyList(),
)
