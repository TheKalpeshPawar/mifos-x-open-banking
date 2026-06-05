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
import kotlinx.coroutines.flow.combine
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
 * (GET /obp/v7.0.0/cards — every card the signed-in user holds). The recent-transactions
 * section is scoped to the CURRENTLY SELECTED card: as the user swipes the carousel the
 * screen calls [selectCard], and we show only the transactions that card made.
 *
 * OBP has no native card→transaction link, so card provenance is carried as a `CARD_ID`
 * transaction-attribute (= the card's `bank_card_number`) seeded on the bank; the v6
 * transactions endpoint returns those attributes inline, so we fetch a card's owning
 * account once ([TransactionsRepository.listTransactionsWithAttributes], cached per account)
 * and filter client-side by [Transaction.cardId]. This separates two cards that share one
 * account — which the account-level transaction list alone cannot do.
 *
 * The card controls (freeze, set limit, view PIN, report lost, order new card) are deferred:
 * OBP exposes them only through management endpoints requiring the CanUpdateCardsForBank role.
 * Tapping one emits a "coming soon" message rather than calling a non-functional endpoint.
 */
class CardsViewModel(
    cardsRepository: CardsRepository,
    private val transactionsRepository: TransactionsRepository,
) : ViewModel() {

    private val stream = cardsRepository.userCardsStream(viewModelScope)

    private val selectedCardNumber = MutableStateFlow("")
    private val transactionsFlow = MutableStateFlow(TransactionsState())

    /** account key "$bankId/$accountId" -> all v6 transactions-with-attributes (fetched once). */
    private val accountTxCache = mutableMapOf<String, List<Transaction>>()

    private val deferredMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val deferredActionMessages: SharedFlow<String> = deferredMessages.asSharedFlow()

    val uiState: StateFlow<ScreenState<CardsContent>> = stream.state
        .combineContent(
            combine(selectedCardNumber, transactionsFlow) { selected, tx -> selected to tx },
        ) { cards, (selected, tx), _ ->
            CardsContent(
                cards = cards,
                selectedCardId = selected.ifBlank { cards.firstOrNull()?.bankCardNumber.orEmpty() },
                recentTransactions = tx.items,
                transactionsLoading = tx.loading,
            )
        }
        .emptyIfContent { it.cards.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    init {
        // Auto-select the first card once the carousel resolves.
        viewModelScope.launch {
            stream.state.collect { state ->
                if (state is ScreenState.Content && selectedCardNumber.value.isBlank()) {
                    state.data.firstOrNull()?.let(::selectCard)
                }
            }
        }
    }

    /** Swiped/tapped to a card — show only that card's transactions (CARD_ID == card number). */
    fun selectCard(card: Card) {
        val alreadyShown =
            selectedCardNumber.value == card.bankCardNumber && transactionsFlow.value.items.isNotEmpty()
        if (alreadyShown) return
        selectedCardNumber.value = card.bankCardNumber

        val accountId = card.account.id
        val bankId = card.account.bankId
        if (accountId.isBlank() || bankId.isBlank()) {
            transactionsFlow.value = TransactionsState()
            return
        }

        val key = "$bankId/$accountId"
        val cached = accountTxCache[key]
        if (cached != null) {
            publishForCard(card.bankCardNumber, cached)
        } else {
            viewModelScope.launch { fetchAndPublish(key, bankId, accountId, card.bankCardNumber) }
        }
    }

    private suspend fun fetchAndPublish(key: String, bankId: String, accountId: String, cardNumber: String) {
        transactionsFlow.value = transactionsFlow.value.copy(loading = true)
        transactionsRepository.listTransactionsWithAttributes(bankId, accountId, limit = FETCH_LIMIT)
            .onSuccess { all ->
                accountTxCache[key] = all
                publishForCard(cardNumber, all)
            }
            .onFailure { transactionsFlow.value = TransactionsState(loading = false) }
    }

    private fun publishForCard(cardNumber: String, all: List<Transaction>) {
        // Only show transactions tagged for this card. If the bank has no CARD_ID tags yet,
        // this is empty by design (the card made no recorded transactions).
        transactionsFlow.value = TransactionsState(loading = false, items = all.forCard(cardNumber, DISPLAY_LIMIT))
    }

    fun onRetry() = stream.retry()

    fun onRefresh() {
        accountTxCache.clear()
        stream.refresh()
    }

    /** Tapping any deferred card control surfaces a "coming soon" message. */
    fun onDeferredAction(control: String) {
        deferredMessages.tryEmit("$control is coming soon")
    }

    private companion object {
        const val FETCH_LIMIT = 100
        const val DISPLAY_LIMIT = 10
    }
}

/** Loaded content for the My Cards screen — the card carousel + the selected card's transactions. */
@Immutable
data class CardsContent(
    val cards: List<Card>,
    val selectedCardId: String,
    val recentTransactions: List<Transaction>,
    val transactionsLoading: Boolean = false,
)

/** Secondary-fetch state for the selected card's transactions (loaded after the cards). */
@Immutable
private data class TransactionsState(
    val loading: Boolean = false,
    val items: List<Transaction> = emptyList(),
)

/**
 * The transactions a specific card made — those whose [Transaction.cardId] (the CARD_ID
 * attribute) equals the card's number — preserving order and capped at [limit]. Pure and
 * order-preserving so two cards sharing one account still yield disjoint lists.
 */
internal fun List<Transaction>.forCard(cardNumber: String, limit: Int): List<Transaction> =
    filter { it.cardId == cardNumber }.take(limit)
